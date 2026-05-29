package com.autocode.gateway.web;

import com.autocode.gateway.security.ProjectAccessEvaluator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ApiGatewaySecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProjectAccessEvaluator projectAccessEvaluator;

    @Test
    void shouldAllowAnonymousAccessToHealthz() throws Exception {
        mockMvc.perform(get("/api/healthz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("api-gateway"));
    }

    @Test
    void shouldReturn401WhenAuthMeWithoutToken() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void shouldReturnDevUserWhenBearerDevTokenProvided() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer dev-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("dev-user"))
                .andExpect(jsonPath("$.username").value("developer"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"));
    }

    @Test
    void shouldReturn400WhenQueryProjectHeaderMissing() throws Exception {
        mockMvc.perform(get("/api/query/ping")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer dev-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PROJECT_ID_REQUIRED"));
    }

    @Test
    void shouldReturn403WhenUserHasNoProjectPermission() throws Exception {
        mockMvc.perform(get("/api/query/ping")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer dev-token")
                        .header("X-Project-Id", "2"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN_PROJECT"));
    }

    @Test
    void shouldAllowQueryWhenUserHasProjectPermission() throws Exception {
        projectAccessEvaluator.grant(3L, "dev-user");
        mockMvc.perform(get("/api/query/ping")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer dev-token")
                        .header("X-Project-Id", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }
}
