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
            select
                d.id as doc_id,
                d.title,
                coalesce(d.metadata_json ->> 'excerpt', d.doc_path) as content_excerpt,
                d.doc_type
            from app.documents d
            where project_id = :projectId
              and (
                  lower(d.title) like :keyword
                  or lower(d.doc_path) like :keyword
                  or lower(coalesce(d.metadata_json ->> 'excerpt', '')) like :keyword
                  or :keyword = '%%'
              )
            limit :limit
            """;
    private static final String LINK_SQL = """
            select
                dl.id as link_id,
                dl.document_id as doc_id,
                case
                    when dl.symbol_id is not null then 'SYMBOL'
                    when dl.commit_id is not null then 'COMMIT'
                    else 'REQUIREMENT'
                end as target_type,
                coalesce(cast(dl.symbol_id as text), cast(dl.commit_id as text), cast(dl.requirement_id as text)) as target_id
            from app.document_links dl
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
        Map<String, Object> params = Map.of(
                "projectId", request.projectId(),
                "keyword", "%" + normalize(request.queryText()) + "%",
                "limit", limit
        );
        List<StandardQueryHit> hits = new ArrayList<>();
        boolean docFailed = false;
        boolean linkFailed = false;
        try {
            List<Map<String, Object>> rows = queryExecutor.queryForList(DOCUMENT_SQL, params);
            for (Map<String, Object> row : rows) {
                hits.add(new StandardQueryHit(
                        QuerySourceType.DOCUMENT,
                        value(row, "doc_id", "id"),
                        value(row, "title"),
                        excerpt(row),
                        1.0d,
                        Map.of("docType", value(row, "doc_type"))
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
                        value(row, "link_id", "id"),
                        "link:" + value(row, "target_type"),
                        value(row, "doc_id", "document_id") + " -> " + value(row, "target_id", "symbol_id", "commit_id", "requirement_id"),
                        0.8d,
                        Map.of("targetType", value(row, "target_type"))
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

    private String normalize(String text) {
        return text == null ? "" : text.trim().toLowerCase();
    }

    private String excerpt(Map<String, Object> row) {
        Object metadata = row.get("metadata_json");
        if (metadata instanceof Map<?, ?> metadataMap) {
            Object excerpt = metadataMap.get("excerpt");
            if (excerpt != null) {
                return String.valueOf(excerpt);
            }
        }
        return value(row, "content_excerpt", "doc_path");
    }

    private String value(Map<String, Object> row, String... keys) {
        for (String key : keys) {
            Object value = row.get(key);
            if (value != null) {
                return String.valueOf(value);
            }
        }
        return "";
    }
}
