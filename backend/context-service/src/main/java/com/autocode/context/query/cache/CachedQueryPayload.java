package com.autocode.context.query.cache;

import com.autocode.context.query.QueryIntent;
import com.autocode.context.query.adapter.StandardQueryResult;
import com.autocode.context.query.context.StructuredContextBundle;

public record CachedQueryPayload(
        QueryIntent intent,
        StandardQueryResult result,
        StructuredContextBundle context
) {
}
