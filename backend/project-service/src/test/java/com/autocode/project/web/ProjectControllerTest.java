package com.autocode.project.web;

import com.autocode.project.support.ProjectServiceIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProjectControllerTest extends ProjectServiceIntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    @Test
    void shouldCreateProject() throws Exception {
        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "autocode-platform",
                                  "repoUrl": "https://git.example.com/team/autocode-platform.git",
                                  "defaultBranch": "main",
                                  "languageStack": "Java,TypeScript",
                                  "docRepoPath": "docs/"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("autocode-platform"))
                .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    void shouldRejectCreateProjectWhenNameMissing() throws Exception {
        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "repoUrl": "https://git.example.com/team/autocode-platform.git",
                                  "defaultBranch": "main"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldRejectCreateProjectWhenNameTooLong() throws Exception {
        String tooLongName = "a".repeat(129);

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "repoUrl": "https://git.example.com/team/autocode-platform.git",
                                  "defaultBranch": "main"
                                }
                                """.formatted(tooLongName)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldReturn409WhenProjectNameDuplicated() throws Exception {
        String payload = """
                {
                  "name": "autocode-platform",
                  "repoUrl": "https://git.example.com/team/autocode-platform.git",
                  "defaultBranch": "main"
                }
                """;

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PROJECT_NAME_CONFLICT"));
    }

    @Test
    void shouldListProjectsInDescendingIdOrder() throws Exception {
        createProject("autocode-core");
        createProject("autocode-web");

        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("autocode-web"))
                .andExpect(jsonPath("$[1].name").value("autocode-core"));
    }

    @Test
    void shouldGetProjectById() throws Exception {
        long projectId = createProject("autocode-platform");

        mockMvc.perform(get("/api/projects/{id}", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(projectId))
                .andExpect(jsonPath("$.name").value("autocode-platform"))
                .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    void shouldReturn404WhenProjectMissing() throws Exception {
        mockMvc.perform(get("/api/projects/{id}", 9999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROJECT_NOT_FOUND"));
    }

    @Test
    void shouldEvictProjectListCacheAfterCreate() throws Exception {
        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isOk());

        assertThat(redisTemplate.hasKey("ac:v1:project-meta:global:project:list")).isTrue();

        createProject("autocode-platform");

        assertThat(redisTemplate.hasKey("ac:v1:project-meta:global:project:list")).isFalse();
    }

    @Test
    void shouldBackfillDetailCacheAfterGetById() throws Exception {
        long projectId = createProject("autocode-platform");

        mockMvc.perform(get("/api/projects/{id}", projectId))
                .andExpect(status().isOk());

        assertThat(redisTemplate.hasKey("ac:v1:project-meta:p:" + projectId + ":project:detail")).isTrue();
    }

    @Test
    void shouldUseIsolatedDatabaseAndRedisLogicalDbForTests() {
        assertThat(jdbcTemplate.queryForObject("select current_database()", String.class))
                .isEqualTo(TEST_DATABASE);
        assertThat(((LettuceConnectionFactory) redisConnectionFactory).getDatabase())
                .isEqualTo(TEST_REDIS_DATABASE);
    }

    @Test
    void shouldGrantAndCheckProjectAccessForUser() throws Exception {
        long projectId = createProject("autocode-perm");

        mockMvc.perform(post("/internal/projects/{projectId}/members/{userId}", projectId, "u1001")
                        .param("role", "owner"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value(projectId))
                .andExpect(jsonPath("$.userId").value("u1001"))
                .andExpect(jsonPath("$.role").value("OWNER"))
                .andExpect(jsonPath("$.allowed").value(true));

        mockMvc.perform(get("/internal/projects/{projectId}/members/{userId}/access", projectId, "u1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("OWNER"))
                .andExpect(jsonPath("$.allowed").value(true));
    }

    @Test
    void shouldDenyAccessWhenUserNotBoundToProject() throws Exception {
        long projectId = createProject("autocode-no-access");

        mockMvc.perform(get("/internal/projects/{projectId}/members/{userId}/access", projectId, "u404"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("NONE"))
                .andExpect(jsonPath("$.allowed").value(false));
    }

    @Test
    void shouldSyncRealIndexesFromWorkspace() throws Exception {
        Path workspace = createGitWorkspace();
        long projectId = createProject("autocode-sync-demo");

        // Async indexing: the response returns immediately with INDEXING status
        mockMvc.perform(post("/api/projects/{id}/sync-indexes", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "workspaceRoot": "%s",
                                  "maxCommits": 20
                                }
                                """.formatted(escapeJson(workspace.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value(projectId))
                .andExpect(jsonPath("$.status").value("INDEXING"));

        // Poll until READY or FAILED
        awaitIndexStatus(projectId, "READY", 30_000);

        assertThat(jdbcTemplate.queryForObject("select count(*) from app.symbols where project_id = ?", Integer.class, projectId))
                .isGreaterThan(0);
        assertThat(jdbcTemplate.queryForObject("select count(*) from app.commits where project_id = ?", Integer.class, projectId))
                .isGreaterThan(0);
        assertThat(jdbcTemplate.queryForObject("select count(*) from app.documents where project_id = ?", Integer.class, projectId))
                .isGreaterThan(0);
    }

    private void awaitIndexStatus(long projectId, String expectedStatus, long timeoutMs) throws Exception {
        long deadline = System.currentTimeMillis() + timeoutMs;
        String status = null;
        while (System.currentTimeMillis() < deadline) {
            String response = mockMvc.perform(get("/api/projects/{id}", projectId))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString(StandardCharsets.UTF_8);
            // Extract status field from JSON response
            int statusIdx = response.indexOf("\"status\"");
            if (statusIdx >= 0) {
                int colonIdx = response.indexOf(":", statusIdx);
                int startQuote = response.indexOf("\"", colonIdx + 1);
                int endQuote = response.indexOf("\"", startQuote + 1);
                if (startQuote >= 0 && endQuote >= 0) {
                    status = response.substring(startQuote + 1, endQuote);
                }
            }
            if (expectedStatus.equals(status) || "FAILED".equals(status)) {
                break;
            }
            Thread.sleep(500);
        }
        assertThat(status).isEqualTo(expectedStatus);
    }

    private long createProject(String name) throws Exception {
        String response = mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "repoUrl": "https://git.example.com/team/%s.git",
                                  "defaultBranch": "main"
                                }
                                """.formatted(name, name)))
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        String idText = response.replaceAll("(?s).*\"id\"\\s*:\\s*(\\d+).*", "$1");
        return Long.parseLong(idText);
    }

    private Path createGitWorkspace() throws Exception {
        Path workspace = Files.createTempDirectory("autocode-index-sync");
        Path sourceDir = workspace.resolve(Path.of("src", "main", "java", "demo"));
        Path docsDir = workspace.resolve(Path.of("docs", "specs"));
        Files.createDirectories(sourceDir);
        Files.createDirectories(docsDir);
        Files.writeString(sourceDir.resolve("ProjectIndexSyncService.java"), """
                package demo;

                public class ProjectIndexSyncService {
                    public void syncIndexes() {
                        persistDocuments();
                    }

                    private void persistDocuments() {
                    }
                }
                """);
        Files.writeString(sourceDir.resolve("ProjectIndexRepository.java"), """
                package demo;

                public class ProjectIndexRepository {
                }
                """);
        Files.writeString(docsDir.resolve("CS-01-sync-index.md"), """
                # CS-01 Sync Index

                This document links CS-01 with ProjectIndexSyncService and real index sync work.
                """);

        runCommand(workspace, "git", "init");
        runCommand(workspace, "git", "config", "user.name", "Codex");
        runCommand(workspace, "git", "config", "user.email", "codex@example.com");
        runCommand(workspace, "git", "add", ".");
        runCommand(workspace, "git", "commit", "-m", "feat: add sync index workspace");
        return workspace;
    }

    private void runCommand(Path workdir, String... command) throws Exception {
        Process process = new ProcessBuilder(command)
                .directory(workdir.toFile())
                .redirectErrorStream(true)
                .start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            String output;
            try (var inputStream = process.getInputStream()) {
                output = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            }
            throw new IllegalStateException("Command failed: " + String.join(" ", command) + System.lineSeparator() + output);
        }
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\");
    }
}
