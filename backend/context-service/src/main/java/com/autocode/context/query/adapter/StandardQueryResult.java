package com.autocode.context.query.adapter;

import java.util.List;

public record StandardQueryResult(
        QueryResultStatus status,
        List<StandardQueryHit> hits,
        String message
) {
    public static StandardQueryResult success(List<StandardQueryHit> hits, String message) {
        return new StandardQueryResult(QueryResultStatus.SUCCESS, hits, message);
    }

    public static StandardQueryResult empty(String message) {
        return new StandardQueryResult(QueryResultStatus.EMPTY, List.of(), message);
    }

    public static StandardQueryResult partial(List<StandardQueryHit> hits, String message) {
        return new StandardQueryResult(QueryResultStatus.PARTIAL, hits, message);
    }

    public static StandardQueryResult error(String message) {
        return new StandardQueryResult(QueryResultStatus.ERROR, List.of(), message);
    }
}
