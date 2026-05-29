package com.autocode.context.query.adapter;

import com.autocode.context.query.ContextQueryRequest;
import com.autocode.context.query.QueryIntent;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class DocumentQueryAdapter implements ContextQueryAdapter {
    private static final String DOCUMENT_SQL = """
            select doc_id, title, content_excerpt, doc_type
            from documents
            where project_id = :projectId
            limit :limit
            """;
    private static final String LINK_SQL = """
            select link_id, doc_id, target_type, target_id
            from document_links
            where project_id = :projectId
            limit :limit
            """;
    private final SqlQueryExecutor queryExecutor;

    public DocumentQueryAdapter(SqlQueryExecutor queryExecutor) {
        this.queryExecutor = queryExecutor;
    }

    @Override
    public boolean supports(QueryIntent intent) {
        return intent == QueryIntent.DOCUMENT_TRACE || intent == QueryIntent.REQUIREMENT_ANALYSIS;
    }

    @Override
    public StandardQueryResult query(ContextQueryRequest request) {
        if (request == null || request.projectId() == null) {
            return StandardQueryResult.error("projectId is required");
        }
        // Documents and links are queried independently so one source failure can degrade to PARTIAL.
        int limit = extractLimit(request);
        Map<String, Object> params = Map.of("projectId", request.projectId(), "limit", limit);
        List<StandardQueryHit> hits = new ArrayList<>();
        boolean docFailed = false;
        boolean linkFailed = false;
        try {
            List<Map<String, Object>> rows = queryExecutor.queryForList(DOCUMENT_SQL, params);
            for (Map<String, Object> row : rows) {
                hits.add(new StandardQueryHit(
                        QuerySourceType.DOCUMENT,
                        value(row.get("doc_id")),
                        value(row.get("title")),
                        value(row.get("content_excerpt")),
                        1.0d,
                        Map.of("docType", value(row.get("doc_type")))
                ));
            }
        } catch (RuntimeException ex) {
            docFailed = true;
        }
        try {
            List<Map<String, Object>> rows = queryExecutor.queryForList(LINK_SQL, params);
            for (Map<String, Object> row : rows) {
                hits.add(new StandardQueryHit(
                        QuerySourceType.DOCUMENT,
                        value(row.get("link_id")),
                        "link:" + value(row.get("target_type")),
                        value(row.get("doc_id")) + " -> " + value(row.get("target_id")),
                        0.8d,
                        Map.of("targetType", value(row.get("target_type")))
                ));
            }
        } catch (RuntimeException ex) {
            linkFailed = true;
        }
        // Shared result-state contract with other adapters: ERROR > EMPTY > PARTIAL > SUCCESS.
        if (docFailed && linkFailed) {
            return StandardQueryResult.error("document query failed");
        }
        if (hits.isEmpty()) {
            return StandardQueryResult.empty("document query returned no records");
        }
        if (docFailed || linkFailed) {
            return StandardQueryResult.partial(hits, "document query partially succeeded");
        }
        return StandardQueryResult.success(hits, "document query succeeded");
    }

    private int extractLimit(ContextQueryRequest request) {
        Object value = request.options() == null ? null : request.options().get("limit");
        if (value instanceof Number number) {
            int limit = number.intValue();
            return Math.max(1, Math.min(limit, 100));
        }
        return 20;
    }

    private String value(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
