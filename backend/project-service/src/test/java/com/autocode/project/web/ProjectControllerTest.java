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

import java.nio.charset.StandardCharsets;

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
}
