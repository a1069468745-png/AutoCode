package com.autocode.context.health;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ContextHealthController.class)
class ContextHealthControllerTest {
    private static final String SUCCESS_CODE = "OK";
    private static final String HEALTH_MESSAGE = "context-service is healthy";
    private static final String READINESS_MESSAGE = "context-service readiness checked";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ContextReadinessService contextReadinessService;

    @Test
    void shouldReturnHealthEnvelope() throws Exception {
        mockMvc.perform(get("/internal/context/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(SUCCESS_CODE))
                .andExpect(jsonPath("$.message").value(HEALTH_MESSAGE))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.data.status").value("UP"));
    }

    @Test
    void shouldIncludeTimestampInHealthResponse() throws Exception {
        mockMvc.perform(get("/internal/context/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void shouldReturnReadinessUpEnvelope() throws Exception {
        given(contextReadinessService.check()).willReturn(
                new ContextReadiness("UP", true, "Redis probe succeeded")
        );

        mockMvc.perform(get("/internal/context/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(SUCCESS_CODE))
                .andExpect(jsonPath("$.message").value(READINESS_MESSAGE))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.redisAvailable").value(true))
                .andExpect(jsonPath("$.data.detail").value("Redis probe succeeded"));
    }

    @Test
    void shouldReturnReadinessDownEnvelope() throws Exception {
        given(contextReadinessService.check()).willReturn(
                new ContextReadiness("DOWN", false, "Redis probe failed")
        );

        mockMvc.perform(get("/internal/context/readiness"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(SUCCESS_CODE))
                .andExpect(jsonPath("$.message").value(READINESS_MESSAGE))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.data.status").value("DOWN"))
                .andExpect(jsonPath("$.data.redisAvailable").value(false))
                .andExpect(jsonPath("$.data.detail").value("Redis probe failed"));
    }
}
