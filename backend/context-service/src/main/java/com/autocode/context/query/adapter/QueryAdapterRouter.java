package com.autocode.context.query.adapter;

import com.autocode.context.query.ContextQueryRequest;
import com.autocode.context.query.QueryIntent;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class QueryAdapterRouter {
    private final List<ContextQueryAdapter> adapters;

    public QueryAdapterRouter(List<ContextQueryAdapter> adapters) {
        this.adapters = adapters;
    }

    public StandardQueryResult route(QueryIntent intent, ContextQueryRequest request) {
        for (ContextQueryAdapter adapter : adapters) {
            if (adapter.supports(intent)) {
                return adapter.query(request);
            }
        }
        return StandardQueryResult.error("no adapter found for intent: " + intent);
    }
}
