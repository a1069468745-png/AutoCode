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
            select c.commit_id, c.author_name, c.message, c.committed_at, cs.symbol_id
            from commits c
            left join commit_symbols cs on c.commit_id = cs.commit_id
            where c.project_id = :projectId
            order by c.committed_at desc
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
            int limit = extractLimit(request);
            List<Map<String, Object>> rows = queryExecutor.queryForList(HISTORY_SQL, Map.of(
                    "projectId", request.projectId(),
                    "limit", limit
            ));
            if (rows.isEmpty()) {
                return StandardQueryResult.empty("history query returned no records");
            }
            List<StandardQueryHit> hits = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                hits.add(new StandardQueryHit(
                        QuerySourceType.HISTORY,
                        value(row.get("commit_id")),
                        value(row.get("message")),
                        value(row.get("author_name")) + " @ " + value(row.get("committed_at")),
                        1.0d,
                        Map.of("symbolId", value(row.get("symbol_id")))
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

    private String value(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
