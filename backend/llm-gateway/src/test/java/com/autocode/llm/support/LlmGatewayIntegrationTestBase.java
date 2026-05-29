package com.autocode.llm.support;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import java.util.Properties;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class LlmGatewayIntegrationTestBase {

    protected static final String TEST_DATABASE = "autocode_llm_gateway_test";

    private static final Path DEPLOY_ENV = Path.of("..", "..", "deploy", ".env").normalize();

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        ensureTestDatabaseExists();
        registry.add("spring.datasource.url", () -> "jdbc:postgresql://127.0.0.1:5432/" + TEST_DATABASE);
        registry.add("spring.datasource.username", () -> "autocode");
        registry.add("spring.datasource.password", () -> resolveSetting("POSTGRES_PASSWORD", "replace_me"));
    }

    @BeforeEach
    void resetSchema(@Autowired JdbcTemplate jdbcTemplate) {
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
                    constraint uq_projects_name unique (name)
                )
                """);
        jdbcTemplate.execute("""
                create table app.model_profiles (
                    id bigserial primary key,
                    project_id bigint not null,
                    provider varchar(64) not null,
                    base_url varchar(512),
                    model_name varchar(128) not null,
                    embedding_model varchar(128),
                    timeout_seconds integer not null default 30,
                    fallback_model varchar(128),
                    enable_local_only boolean not null default false,
                    created_at timestamptz not null default now(),
                    updated_at timestamptz not null default now(),
                    constraint fk_model_profiles_project
                        foreign key (project_id) references app.projects (id) on delete cascade,
                    constraint uq_model_profiles_project unique (project_id),
                    constraint chk_model_profiles_timeout_seconds check (timeout_seconds > 0)
                )
                """);
        jdbcTemplate.execute("""
                create table app.query_logs (
                    id bigserial primary key,
                    project_id bigint not null,
                    user_id varchar(128) not null,
                    query_type varchar(64) not null,
                    query_text text not null,
                    model_used varchar(128),
                    cost_token bigint not null default 0,
                    created_at timestamptz not null default now(),
                    constraint fk_query_logs_project
                        foreign key (project_id) references app.projects (id) on delete cascade,
                    constraint chk_query_logs_cost_token check (cost_token >= 0)
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
