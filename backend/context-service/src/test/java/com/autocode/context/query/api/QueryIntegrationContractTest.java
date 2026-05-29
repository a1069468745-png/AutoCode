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
class QueryIntegrationContractTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private QueryOrchestrationService queryOrchestrationService;

    @Test
    void shouldKeepConsistentEnvelopeAcrossAllQueryEndpoints() throws Exception {
        given(queryOrchestrationService.execute(any(ContextQueryRequest.class), any()))
                .willReturn(payload(QueryIntent.CODE_LOCATE, false));

        // Core contract check: all query endpoints return the same top-level fields and context payload.
        assertEnvelope("/api/query/code", "{\"projectId\":1,\"queryText\":\"locate\"}");
        assertEnvelope("/api/query/history", "{\"projectId\":1,\"queryText\":\"history\"}");
        assertEnvelope("/api/query/knowledge", "{\"projectId\":1,\"queryText\":\"doc\"}");
        assertEnvelope("/api/query/impact", "{\"projectId\":1,\"queryText\":\"impact\"}");
        assertEnvelope("/api/query/traceability", "{\"projectId\":1,\"queryText\":\"trace\"}");
        assertEnvelope("/api/query/ask", "{\"projectId\":1,\"queryText\":\"ask\"}");
    }

    @Test
    void shouldRouteToExpectedIntentsForDedicatedEndpoints() throws Exception {
        given(queryOrchestrationService.execute(any(ContextQueryRequest.class), eq(QueryIntent.CODE_LOCATE)))
                .willReturn(payload(QueryIntent.CODE_LOCATE, false));
        given(queryOrchestrationService.execute(any(ContextQueryRequest.class), eq(QueryIntent.HISTORY_TRACE)))
                .willReturn(payload(QueryIntent.HISTORY_TRACE, false));
        given(queryOrchestrationService.execute(any(ContextQueryRequest.class), eq(QueryIntent.DOCUMENT_TRACE)))
                .willReturn(payload(QueryIntent.DOCUMENT_TRACE, false));
        given(queryOrchestrationService.execute(any(ContextQueryRequest.class), eq(QueryIntent.CALL_RELATION)))
                .willReturn(payload(QueryIntent.CALL_RELATION, false));
        given(queryOrchestrationService.execute(any(ContextQueryRequest.class), eq(QueryIntent.REQUIREMENT_ANALYSIS)))
                .willReturn(payload(QueryIntent.REQUIREMENT_ANALYSIS, false));
        given(queryOrchestrationService.execute(any(ContextQueryRequest.class), eq(null)))
                .willReturn(payload(QueryIntent.CODE_LOCATE, true));

        mockMvc.perform(post("/api/query/code").contentType(MediaType.APPLICATION_JSON).content("{\"projectId\":1,\"queryText\":\"locate\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.intent").value("CODE_LOCATE"));
        mockMvc.perform(post("/api/query/history").contentType(MediaType.APPLICATION_JSON).content("{\"projectId\":1,\"queryText\":\"history\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.intent").value("HISTORY_TRACE"));
        mockMvc.perform(post("/api/query/knowledge").contentType(MediaType.APPLICATION_JSON).content("{\"projectId\":1,\"queryText\":\"doc\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.intent").value("DOCUMENT_TRACE"));
        mockMvc.perform(post("/api/query/impact").contentType(MediaType.APPLICATION_JSON).content("{\"projectId\":1,\"queryText\":\"impact\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.intent").value("CALL_RELATION"));
        mockMvc.perform(post("/api/query/traceability").contentType(MediaType.APPLICATION_JSON).content("{\"projectId\":1,\"queryText\":\"trace\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.intent").value("REQUIREMENT_ANALYSIS"));
        mockMvc.perform(post("/api/query/ask").contentType(MediaType.APPLICATION_JSON).content("{\"projectId\":1,\"queryText\":\"ask\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cacheHit").value(true));
    }

    private void assertEnvelope(String path, String body) throws Exception {
        mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.message").value("query executed"))
                .andExpect(jsonPath("$.data.result.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.context.facts[0]").value("fact"))
                .andExpect(jsonPath("$.data.context.relatedNodes[0]").value("node-1"))
                .andExpect(jsonPath("$.data.context.historySummary").value("history summary"))
                .andExpect(jsonPath("$.data.context.documentSummary").value("document summary"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    private QueryResponsePayload payload(QueryIntent intent, boolean cacheHit) {
        return new QueryResponsePayload(
                intent,
                new StandardQueryResult(
                        QueryResultStatus.SUCCESS,
                        List.of(new StandardQueryHit(QuerySourceType.CODE_GRAPH, "node-1", "title", "snippet", 1.0d, Map.of())),
                        "ok"
                ),
                new StructuredContextBundle(
                        List.of("fact"),
                        List.of("node-1"),
                        "history summary",
                        "document summary"
                ),
                cacheHit
        );
    }
}
