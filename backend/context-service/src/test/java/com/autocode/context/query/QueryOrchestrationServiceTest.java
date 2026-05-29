package com.autocode.context.query;

import com.autocode.context.query.adapter.QueryAdapterRouter;
import com.autocode.context.query.adapter.StandardQueryResult;
import com.autocode.context.query.api.QueryResponsePayload;
import com.autocode.context.query.cache.CachedQueryPayload;
import com.autocode.context.query.cache.QueryCacheService;
import com.autocode.context.query.context.ContextAggregationService;
import com.autocode.context.query.context.StructuredContextBundle;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class QueryOrchestrationServiceTest {
    @Test
    void shouldReturnCachedPayloadWhenHit() {
        QueryIntentResolver resolver = mock(QueryIntentResolver.class);
        QueryAdapterRouter router = mock(QueryAdapterRouter.class);
        ContextAggregationService aggregationService = mock(ContextAggregationService.class);
        QueryCacheService cacheService = mock(QueryCacheService.class);
        ContextQueryRequest request = new ContextQueryRequest(1L, "find", null, Map.of());
        StandardQueryResult result = StandardQueryResult.empty("none");
        StructuredContextBundle bundle = new StructuredContextBundle(List.of(), List.of(), "h", "d");

        given(cacheService.get(any(ContextQueryRequest.class), eq(QueryIntent.CODE_LOCATE)))
                .willReturn(Optional.of(new CachedQueryPayload(QueryIntent.CODE_LOCATE, result, bundle)));

        QueryOrchestrationService service = new QueryOrchestrationService(resolver, router, aggregationService, cacheService);
        QueryResponsePayload payload = service.execute(request, QueryIntent.CODE_LOCATE);

        assertTrue(payload.cacheHit());
    }

    @Test
    void shouldQueryAndCacheWhenMiss() {
        QueryIntentResolver resolver = mock(QueryIntentResolver.class);
        QueryAdapterRouter router = mock(QueryAdapterRouter.class);
        ContextAggregationService aggregationService = mock(ContextAggregationService.class);
        QueryCacheService cacheService = mock(QueryCacheService.class);
        ContextQueryRequest request = new ContextQueryRequest(1L, "find", null, Map.of());
        StandardQueryResult result = StandardQueryResult.empty("none");
        StructuredContextBundle bundle = new StructuredContextBundle(List.of(), List.of(), "h", "d");

        given(cacheService.get(any(ContextQueryRequest.class), eq(QueryIntent.CODE_LOCATE)))
                .willReturn(Optional.empty());
        given(router.route(eq(QueryIntent.CODE_LOCATE), any(ContextQueryRequest.class))).willReturn(result);
        given(aggregationService.aggregate(eq(QueryIntent.CODE_LOCATE), eq(result))).willReturn(bundle);

        QueryOrchestrationService service = new QueryOrchestrationService(resolver, router, aggregationService, cacheService);
        QueryResponsePayload payload = service.execute(request, QueryIntent.CODE_LOCATE);

        assertFalse(payload.cacheHit());
        verify(cacheService).put(eq(request), eq(QueryIntent.CODE_LOCATE), any(CachedQueryPayload.class));
    }
}
