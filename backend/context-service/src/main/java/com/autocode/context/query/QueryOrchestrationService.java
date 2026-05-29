package com.autocode.context.query;

import com.autocode.context.query.adapter.QueryAdapterRouter;
import com.autocode.context.query.api.QueryResponsePayload;
import org.springframework.stereotype.Service;

@Service
public class QueryOrchestrationService {
    private final QueryIntentResolver queryIntentResolver;
    private final QueryAdapterRouter queryAdapterRouter;

    public QueryOrchestrationService(QueryIntentResolver queryIntentResolver, QueryAdapterRouter queryAdapterRouter) {
        this.queryIntentResolver = queryIntentResolver;
        this.queryAdapterRouter = queryAdapterRouter;
    }

    public QueryResponsePayload execute(ContextQueryRequest request, QueryIntent forcedIntent) {
        QueryIntent intent = forcedIntent;
        if (intent == null) {
            intent = queryIntentResolver.resolve(request).intent();
        }
        return new QueryResponsePayload(intent, queryAdapterRouter.route(intent, request));
    }
}
