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
