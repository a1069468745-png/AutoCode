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
            select id as symbol_id, symbol_name as name, symbol_kind as kind, file_path
            from app.symbols
            where project_id = :projectId
              and (
                  lower(symbol_name) like :keyword
                  or lower(file_path) like :keyword
              )
            limit :limit
            """;
    private static final String EDGE_SQL = """
            select
                se.id as edge_id,
                coalesce(src.symbol_name, cast(se.source_symbol_id as text)) as src_symbol_id,
                coalesce(dst.symbol_name, cast(se.target_symbol_id as text)) as dst_symbol_id,
                se.edge_type
            from app.symbol_edges se
            left join app.symbols src on src.id = se.source_symbol_id
            left join app.symbols dst on dst.id = se.target_symbol_id
            where se.project_id = :projectId
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
                        value(row, "symbol_id", "id"),
                        value(row, "name", "symbol_name"),
                        value(row, "file_path"),
                        1.0d,
                        Map.of("kind", value(row, "kind", "symbol_kind"))
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
                        value(row, "edge_id", "id"),
                        "edge:" + value(row, "edge_type"),
                        value(row, "src_symbol_id", "source_symbol_id") + " -> " + value(row, "dst_symbol_id", "target_symbol_id"),
                        0.8d,
                        Map.of("edgeType", value(row, "edge_type"))
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
