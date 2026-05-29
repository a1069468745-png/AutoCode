package com.autocode.context.query;

import java.util.Map;

public record ContextQueryRequest(
        Long projectId,
        String queryText,
        QueryIntent preferredIntent,
        Map<String, Object> options
) {
}
