package com.autocode.project.support;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class ProjectServiceIntegrationTestBase {

    protected static final String TEST_DATABASE = "autocode_project_service_test";
    protected static final int TEST_REDIS_DATABASE = 15;

    private static final Path DEPLOY_ENV = Path.of("..", "..", "deploy", ".env").normalize();

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        ensureTestDatabaseExists();
        registry.add("spring.datasource.url", () -> "jdbc:postgresql://127.0.0.1:5432/" + TEST_DATABASE);
        registry.add("spring.datasource.username", () -> "autocode");
        registry.add("spring.datasource.password", () -> resolveSetting("POSTGRES_PASSWORD", "replace_me"));
        registry.add("spring.data.redis.host", () -> "127.0.0.1");
        registry.add("spring.data.redis.port", () -> 6379);
        registry.add("spring.data.redis.password", () -> resolveSetting("REDIS_PASSWORD", "replace_me"));
        registry.add("spring.data.redis.database", () -> TEST_REDIS_DATABASE);
    }

    @BeforeEach
    void resetSchema(@Autowired JdbcTemplate jdbcTemplate,
                     @Autowired StringRedisTemplate redisTemplate) {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
        jdbcTemplate.execute("drop schema if exists app cascade");
        jdbcTemplate.execute("create schema app");
        jdbcTemplate.execute("""
                create table app.projects (
                    id bigserial primary key,
                    name varchar(128) not null,
                    repo_url varchar(512) not null,
                    default_branch varchar(128) not null,
                    language_stack varchar(256),
                    doc_repo_path varchar(512),
                    status varchar(32) not null default 'CREATED',
                    created_at timestamptz not null default now(),
                    updated_at timestamptz not null default now(),
                    constraint uq_projects_name unique (name),
                    constraint chk_projects_status check (
                        status in ('CREATED', 'INDEXING', 'READY', 'FAILED', 'DISABLED')
                    )
                )
                """);
        jdbcTemplate.execute("""
                create table app.documents (
                    id bigserial primary key,
                    project_id bigint not null,
                    doc_path varchar(512) not null,
                    doc_type varchar(64) not null,
                    title varchar(256) not null,
                    metadata_json jsonb not null default '{}'::jsonb,
                    created_at timestamptz not null default now(),
                    updated_at timestamptz not null default now(),
                    constraint fk_documents_project
                        foreign key (project_id) references app.projects (id) on delete cascade,
                    constraint uq_documents_project_doc_path unique (project_id, doc_path)
                )
                """);
        jdbcTemplate.execute("""
                create table app.requirements (
                    id bigserial primary key,
                    project_id bigint not null,
                    requirement_code varchar(128) not null,
                    title varchar(256) not null,
                    status varchar(32) not null default 'DRAFT',
                    source_doc_id bigint,
                    created_at timestamptz not null default now(),
                    updated_at timestamptz not null default now(),
                    constraint fk_requirements_project
                        foreign key (project_id) references app.projects (id) on delete cascade,
                    constraint fk_requirements_source_doc
                        foreign key (source_doc_id) references app.documents (id) on delete set null,
                    constraint uq_requirements_project_code unique (project_id, requirement_code)
                )
                """);
        jdbcTemplate.execute("""
                create table app.symbols (
                    id bigserial primary key,
                    project_id bigint not null,
                    file_path varchar(512) not null,
                    symbol_name varchar(256) not null,
                    symbol_kind varchar(64) not null,
                    signature text,
                    line_start integer not null,
                    line_end integer not null,
                    created_at timestamptz not null default now(),
                    constraint fk_symbols_project
                        foreign key (project_id) references app.projects (id) on delete cascade
                )
                """);
        jdbcTemplate.execute("""
                create table app.commits (
                    id bigserial primary key,
                    project_id bigint not null,
                    commit_hash varchar(64) not null,
                    author varchar(128) not null,
                    commit_time timestamptz not null,
                    message text not null,
                    branch_name varchar(128) not null,
                    created_at timestamptz not null default now(),
                    constraint fk_commits_project
                        foreign key (project_id) references app.projects (id) on delete cascade,
                    constraint uq_commits_project_hash unique (project_id, commit_hash)
                )
                """);
        jdbcTemplate.execute("""
                create table app.symbol_edges (
                    id bigserial primary key,
                    project_id bigint not null,
                    source_symbol_id bigint not null,
                    target_symbol_id bigint not null,
                    edge_type varchar(32) not null,
                    created_at timestamptz not null default now(),
                    constraint fk_symbol_edges_project
                        foreign key (project_id) references app.projects (id) on delete cascade,
                    constraint fk_symbol_edges_source_symbol
                        foreign key (source_symbol_id) references app.symbols (id) on delete cascade,
                    constraint fk_symbol_edges_target_symbol
                        foreign key (target_symbol_id) references app.symbols (id) on delete cascade
                )
                """);
        jdbcTemplate.execute("""
                create table app.commit_symbols (
                    id bigserial primary key,
                    project_id bigint not null,
                    commit_id bigint not null,
                    symbol_id bigint not null,
                    change_type varchar(32) not null,
                    created_at timestamptz not null default now(),
                    constraint fk_commit_symbols_project
                        foreign key (project_id) references app.projects (id) on delete cascade,
                    constraint fk_commit_symbols_commit
                        foreign key (commit_id) references app.commits (id) on delete cascade,
                    constraint fk_commit_symbols_symbol
                        foreign key (symbol_id) references app.symbols (id) on delete cascade
                )
                """);
        jdbcTemplate.execute("""
                create table app.document_links (
                    id bigserial primary key,
                    project_id bigint not null,
                    document_id bigint not null,
                    symbol_id bigint,
                    commit_id bigint,
                    requirement_id bigint,
                    created_at timestamptz not null default now(),
                    constraint fk_document_links_project
                        foreign key (project_id) references app.projects (id) on delete cascade,
                    constraint fk_document_links_document
                        foreign key (document_id) references app.documents (id) on delete cascade,
                    constraint fk_document_links_symbol
                        foreign key (symbol_id) references app.symbols (id) on delete cascade,
                    constraint fk_document_links_commit
                        foreign key (commit_id) references app.commits (id) on delete cascade,
                    constraint fk_document_links_requirement
                        foreign key (requirement_id) references app.requirements (id) on delete cascade
                )
                """);
    }

    private static void ensureTestDatabaseExists() {
        String username = "autocode";
        String password = resolveSetting("POSTGRES_PASSWORD", "replace_me");
        try (Connection connection = DriverManager.getConnection(
                "jdbc:postgresql://127.0.0.1:5432/postgres",
                username,
                password
        );
             Statement statement = connection.createStatement()) {
            statement.execute("create database " + TEST_DATABASE);
        } catch (SQLException exception) {
            if (!"42P04".equals(exception.getSQLState())) {
                throw new IllegalStateException("Failed to create isolated test database", exception);
            }
        }
    }

    private static String resolveSetting(String key, String defaultValue) {
        String envValue = System.getenv(key);
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }

        return readDeployEnv(key).orElse(defaultValue);
    }

    private static Optional<String> readDeployEnv(String key) {
        if (!Files.exists(DEPLOY_ENV)) {
            return Optional.empty();
        }

        Properties properties = new Properties();
        try {
            for (String line : Files.readAllLines(DEPLOY_ENV)) {
                if (line.isBlank() || line.startsWith("#") || !line.contains("=")) {
                    continue;
                }
                int separatorIndex = line.indexOf('=');
                properties.setProperty(
                        line.substring(0, separatorIndex).trim(),
                        line.substring(separatorIndex + 1).trim()
                );
            }
        } catch (IOException exception) {
            return Optional.empty();
        }

        return Optional.ofNullable(properties.getProperty(key)).filter(value -> !value.isBlank());
    }
}
