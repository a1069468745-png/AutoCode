package com.autocode.context.query;

import com.autocode.context.query.adapter.QueryAdapterRouter;
import com.autocode.context.query.cache.CachedQueryPayload;
import com.autocode.context.query.cache.QueryCacheService;
import com.autocode.context.query.api.QueryResponsePayload;
import com.autocode.context.query.context.ContextAggregationService;
import com.autocode.context.query.context.StructuredContextBundle;
import org.springframework.stereotype.Service;

@Service
public class QueryOrchestrationService {
    private final QueryIntentResolver queryIntentResolver;
    private final QueryAdapterRouter queryAdapterRouter;
    private final ContextAggregationService contextAggregationService;
    private final QueryCacheService queryCacheService;

    public QueryOrchestrationService(
            QueryIntentResolver queryIntentResolver,
            QueryAdapterRouter queryAdapterRouter,
            ContextAggregationService contextAggregationService,
            QueryCacheService queryCacheService
    ) {
        this.queryIntentResolver = queryIntentResolver;
        this.queryAdapterRouter = queryAdapterRouter;
        this.contextAggregationService = contextAggregationService;
        this.queryCacheService = queryCacheService;
    }

    public QueryResponsePayload execute(ContextQueryRequest request, QueryIntent forcedIntent) {
        QueryIntent intent = forcedIntent;
        // /ask endpoint depends on intent inference, while typed endpoints force fixed intents.
        if (intent == null) {
            intent = queryIntentResolver.resolve(request).intent();
        }
        // Cache lookup happens after intent resolution so key shape is stable across /ask and typed routes.
        var cached = queryCacheService.get(request, intent);
        if (cached.isPresent()) {
            CachedQueryPayload payload = cached.get();
            return new QueryResponsePayload(payload.intent(), payload.result(), payload.context(), true);
        }
        var result = queryAdapterRouter.route(intent, request);
        // Context bundle is the stable fact package for downstream LLM and analysis flows.
        StructuredContextBundle context = contextAggregationService.aggregate(intent, result);
        queryCacheService.put(request, intent, new CachedQueryPayload(intent, result, context));
        return new QueryResponsePayload(intent, result, context, false);
    }
}
