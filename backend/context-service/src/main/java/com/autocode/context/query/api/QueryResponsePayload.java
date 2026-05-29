package com.autocode.context.query.api;

import com.autocode.context.query.QueryIntent;
import com.autocode.context.query.adapter.StandardQueryResult;

public record QueryResponsePayload(
        QueryIntent intent,
        StandardQueryResult result
) {
}
