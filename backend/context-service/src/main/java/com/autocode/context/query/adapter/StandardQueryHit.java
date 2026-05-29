package com.autocode.context.query.adapter;

import java.util.Map;

public record StandardQueryHit(
        QuerySourceType sourceType,
        String id,
        String title,
        String snippet,
        Double score,
        Map<String, Object> metadata
) {
}
