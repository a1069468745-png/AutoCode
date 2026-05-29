package com.autocode.context.query.api;

import com.autocode.context.query.ContextQueryRequest;
import com.autocode.context.query.QueryIntent;
import com.autocode.context.query.QueryOrchestrationService;
import com.autocode.context.query.adapter.QueryResultStatus;
import com.autocode.context.query.adapter.QuerySourceType;
import com.autocode.context.query.adapter.StandardQueryHit;
import com.autocode.context.query.adapter.StandardQueryResult;
import com.autocode.context.query.context.StructuredContextBundle;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ContextQueryController.class)
class ContextQueryControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private QueryOrchestrationService queryOrchestrationService;

    @Test
    void codeEndpointUsesCodeIntent() throws Exception {
        given(queryOrchestrationService.execute(any(ContextQueryRequest.class), eq(QueryIntent.CODE_LOCATE)))
                .willReturn(payload(QueryIntent.CODE_LOCATE));

        mockMvc.perform(post("/api/query/code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"projectId\":1,\"queryText\":\"find user\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.intent").value("CODE_LOCATE"))
                .andExpect(jsonPath("$.data.result.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.cacheHit").value(false))
                .andExpect(jsonPath("$.data.context.facts[0]").value("title: snippet"));
    }

    @Test
    void askEndpointUsesResolverIntent() throws Exception {
        given(queryOrchestrationService.execute(any(ContextQueryRequest.class), eq(null)))
                .willReturn(payload(QueryIntent.HISTORY_TRACE));

        mockMvc.perform(post("/api/query/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"projectId\":1,\"queryText\":\"最近谁改过这个方法\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.intent").value("HISTORY_TRACE"))
                .andExpect(jsonPath("$.data.result.hits[0].sourceType").value("CODE_GRAPH"));
    }

    @Test
    void impactEndpointUsesCallRelationIntent() throws Exception {
        given(queryOrchestrationService.execute(any(ContextQueryRequest.class), eq(QueryIntent.CALL_RELATION)))
                .willReturn(payload(QueryIntent.CALL_RELATION));

        mockMvc.perform(post("/api/query/impact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"projectId\":1,\"queryText\":\"影响范围\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.intent").value("CALL_RELATION"));
    }

    @Test
    void traceabilityEndpointUsesRequirementIntent() throws Exception {
        given(queryOrchestrationService.execute(any(ContextQueryRequest.class), eq(QueryIntent.REQUIREMENT_ANALYSIS)))
                .willReturn(payload(QueryIntent.REQUIREMENT_ANALYSIS));

        mockMvc.perform(post("/api/query/traceability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"projectId\":1,\"queryText\":\"追溯需求\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.intent").value("REQUIREMENT_ANALYSIS"));
    }

    private QueryResponsePayload payload(QueryIntent intent) {
        StandardQueryResult result = new StandardQueryResult(
                QueryResultStatus.SUCCESS,
                List.of(new StandardQueryHit(
                        QuerySourceType.CODE_GRAPH,
                        "id-1",
                        "title",
                        "snippet",
                        1.0d,
                        Map.of()
                )),
                "ok"
        );
        return new QueryResponsePayload(
                intent,
                result,
                new StructuredContextBundle(
                        List.of("title: snippet"),
                        List.of("id-1"),
                        "history summary",
                        "document summary"
                ),
                false
        );
    }
}
