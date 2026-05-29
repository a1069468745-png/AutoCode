package com.autocode.context.query.adapter;

import com.autocode.context.query.ContextQueryRequest;
import com.autocode.context.query.QueryIntent;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class HistoryQueryAdapter implements ContextQueryAdapter {
    private static final String HISTORY_SQL = """
            select c.id as commit_id, c.author as author_name, c.message, c.commit_time as committed_at, cs.symbol_id
            from app.commits c
            left join app.commit_symbols cs on c.id = cs.commit_id
            where c.project_id = :projectId
              and (
                  lower(c.message) like :keyword
                  or lower(c.author) like :keyword
                  or :keyword = '%%'
              )
            order by c.commit_time desc
            limit :limit
            """;
    private final SqlQueryExecutor queryExecutor;

    public HistoryQueryAdapter(SqlQueryExecutor queryExecutor) {
        this.queryExecutor = queryExecutor;
    }

    @Override
    public boolean supports(QueryIntent intent) {
        return intent == QueryIntent.HISTORY_TRACE;
    }

    @Override
    public StandardQueryResult query(ContextQueryRequest request) {
        if (request == null || request.projectId() == null) {
            return StandardQueryResult.error("projectId is required");
        }
        try {
            // History query normalizes commit + symbol linkage into a source-agnostic hit model.
            int limit = extractLimit(request);
            List<Map<String, Object>> rows = queryExecutor.queryForList(HISTORY_SQL, Map.of(
                    "projectId", request.projectId(),
                    "keyword", "%" + normalize(request.queryText()) + "%",
                    "limit", limit
            ));
            if (rows.isEmpty()) {
                return StandardQueryResult.empty("history query returned no records");
            }
            List<StandardQueryHit> hits = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                hits.add(new StandardQueryHit(
                        QuerySourceType.HISTORY,
                        value(row, "commit_id", "id"),
                        value(row, "message"),
                        value(row, "author_name", "author") + " @ " + value(row, "committed_at", "commit_time"),
                        1.0d,
                        Map.of("symbolId", value(row, "symbol_id"))
                ));
            }
            return StandardQueryResult.success(hits, "history query succeeded");
        } catch (RuntimeException ex) {
            return StandardQueryResult.error("history query failed");
        }
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
