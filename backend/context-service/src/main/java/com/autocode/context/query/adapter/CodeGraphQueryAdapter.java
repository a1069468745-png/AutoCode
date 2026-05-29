package com.autocode.context.query.adapter;

import com.autocode.context.query.ContextQueryRequest;
import com.autocode.context.query.QueryIntent;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class CodeGraphQueryAdapter implements ContextQueryAdapter {
    private static final String SYMBOL_SQL = """
            select symbol_id, name, kind, file_path
            from symbols
            where project_id = :projectId
              and lower(name) like :keyword
            limit :limit
            """;
    private static final String EDGE_SQL = """
            select edge_id, src_symbol_id, dst_symbol_id, edge_type
            from symbol_edges
            where project_id = :projectId
            limit :limit
            """;
    private final SqlQueryExecutor queryExecutor;

    public CodeGraphQueryAdapter(SqlQueryExecutor queryExecutor) {
        this.queryExecutor = queryExecutor;
    }

    @Override
    public boolean supports(QueryIntent intent) {
        return intent == QueryIntent.CODE_LOCATE || intent == QueryIntent.CALL_RELATION;
    }

    @Override
    public StandardQueryResult query(ContextQueryRequest request) {
        if (request == null || request.projectId() == null) {
            return StandardQueryResult.error("projectId is required");
        }
        // One logical query fans out to symbol and edge sources, then merges into one hit list.
        int limit = extractLimit(request);
        String keyword = "%" + normalize(request.queryText()) + "%";
        Map<String, Object> params = Map.of("projectId", request.projectId(), "keyword", keyword, "limit", limit);
        List<StandardQueryHit> hits = new ArrayList<>();
        boolean symbolFailed = false;
        boolean edgeFailed = false;
        try {
            List<Map<String, Object>> rows = queryExecutor.queryForList(SYMBOL_SQL, params);
            for (Map<String, Object> row : rows) {
                hits.add(new StandardQueryHit(
                        QuerySourceType.CODE_GRAPH,
                        value(row.get("symbol_id")),
                        value(row.get("name")),
                        value(row.get("file_path")),
                        1.0d,
                        Map.of("kind", value(row.get("kind")))
                ));
            }
        } catch (RuntimeException ex) {
            symbolFailed = true;
        }
        try {
            List<Map<String, Object>> rows = queryExecutor.queryForList(EDGE_SQL, Map.of("projectId", request.projectId(), "limit", limit));
            for (Map<String, Object> row : rows) {
                hits.add(new StandardQueryHit(
                        QuerySourceType.CODE_GRAPH,
                        value(row.get("edge_id")),
                        "edge:" + value(row.get("edge_type")),
                        value(row.get("src_symbol_id")) + " -> " + value(row.get("dst_symbol_id")),
                        0.8d,
                        Map.of("edgeType", value(row.get("edge_type")))
                ));
            }
        } catch (RuntimeException ex) {
            edgeFailed = true;
        }
        // Standardized status mapping keeps EMPTY/PARTIAL/ERROR semantics consistent across adapters.
        if (symbolFailed && edgeFailed) {
            return StandardQueryResult.error("code graph query failed");
        }
        if (hits.isEmpty()) {
            return StandardQueryResult.empty("code graph query returned no records");
        }
        if (symbolFailed || edgeFailed) {
            return StandardQueryResult.partial(hits, "code graph query partially succeeded");
        }
        return StandardQueryResult.success(hits, "code graph query succeeded");
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

    private String value(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
