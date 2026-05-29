package com.autocode.context.query.context;

import com.autocode.context.query.QueryIntent;
import com.autocode.context.query.adapter.QueryResultStatus;
import com.autocode.context.query.adapter.QuerySourceType;
import com.autocode.context.query.adapter.StandardQueryHit;
import com.autocode.context.query.adapter.StandardQueryResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContextAggregationServiceTest {
    private final ContextAggregationService aggregationService = new ContextAggregationService();

    @Test
    void shouldAggregateCallRelationWithHistoryForIt06() {
        StandardQueryResult result = new StandardQueryResult(
                QueryResultStatus.SUCCESS,
                List.of(
                        new StandardQueryHit(QuerySourceType.CODE_GRAPH, "S-1", "caller -> callee", "serviceA -> serviceB", 1.0d, Map.of()),
                        new StandardQueryHit(QuerySourceType.HISTORY, "C-1", "Fix call path", "alice @ 2026-05-29", 1.0d, Map.of())
                ),
                "ok"
        );

        StructuredContextBundle bundle = aggregationService.aggregate(QueryIntent.CALL_RELATION, result);

        assertEquals(2, bundle.facts().size());
        assertTrue(bundle.relatedNodes().contains("S-1"));
        assertTrue(bundle.historySummary().contains("alice"));
    }

    @Test
    void shouldAggregateRequirementTraceabilityForIt07() {
        StandardQueryResult result = new StandardQueryResult(
                QueryResultStatus.SUCCESS,
                List.of(
                        new StandardQueryHit(QuerySourceType.DOCUMENT, "D-1", "REQ-1001", "new payment flow", 1.0d, Map.of()),
                        new StandardQueryHit(QuerySourceType.DOCUMENT, "L-1", "link:symbol", "D-1 -> PaymentService", 0.8d, Map.of())
                ),
                "ok"
        );

        StructuredContextBundle bundle = aggregationService.aggregate(QueryIntent.REQUIREMENT_ANALYSIS, result);

        assertEquals(2, bundle.facts().size());
        assertTrue(bundle.documentSummary().contains("REQ-1001"));
    }
}
