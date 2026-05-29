package com.autocode.context.query.adapter;

import com.autocode.context.query.ContextQueryRequest;
import com.autocode.context.query.QueryIntent;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QueryAdaptersTest {
    @Test
    void codeGraphReturnsPartialWhenOneQueryFails() {
        StubExecutor executor = new StubExecutor();
        executor.addRows("symbols", List.of(Map.of("symbol_id", "S1", "name", "UserService", "kind", "class", "file_path", "a.java")));
        executor.fail("symbol_edges");
        CodeGraphQueryAdapter adapter = new CodeGraphQueryAdapter(executor);

        StandardQueryResult result = adapter.query(new ContextQueryRequest(1L, "user", QueryIntent.CODE_LOCATE, Map.of()));

        assertEquals(QueryResultStatus.PARTIAL, result.status());
        assertEquals(1, result.hits().size());
    }

    @Test
    void documentReturnsEmptyWhenNoData() {
        StubExecutor executor = new StubExecutor();
        executor.addRows("documents", List.of());
        executor.addRows("document_links", List.of());
        DocumentQueryAdapter adapter = new DocumentQueryAdapter(executor);

        StandardQueryResult result = adapter.query(new ContextQueryRequest(1L, "design", QueryIntent.DOCUMENT_TRACE, Map.of()));

        assertEquals(QueryResultStatus.EMPTY, result.status());
        assertEquals(0, result.hits().size());
    }

    @Test
    void historyReturnsErrorOnFailure() {
        StubExecutor executor = new StubExecutor();
        executor.fail("commits");
        HistoryQueryAdapter adapter = new HistoryQueryAdapter(executor);

        StandardQueryResult result = adapter.query(new ContextQueryRequest(1L, "history", QueryIntent.HISTORY_TRACE, Map.of()));

        assertEquals(QueryResultStatus.ERROR, result.status());
    }

    @Test
    void routerDispatchesByIntent() {
        StubExecutor executor = new StubExecutor();
        executor.addRows("documents", List.of(Map.of("doc_id", "D1", "title", "Spec", "content_excerpt", "desc", "doc_type", "spec")));
        executor.addRows("document_links", List.of());

        QueryAdapterRouter router = new QueryAdapterRouter(List.of(
                new CodeGraphQueryAdapter(executor),
                new HistoryQueryAdapter(executor),
                new DocumentQueryAdapter(executor)
        ));

        StandardQueryResult result = router.route(
                QueryIntent.DOCUMENT_TRACE,
                new ContextQueryRequest(1L, "spec", QueryIntent.DOCUMENT_TRACE, Map.of())
        );
        assertEquals(QueryResultStatus.SUCCESS, result.status());
        assertEquals(QuerySourceType.DOCUMENT, result.hits().get(0).sourceType());
    }

    private static class StubExecutor implements SqlQueryExecutor {
        private final Map<String, List<Map<String, Object>>> rowsByToken = new HashMap<>();
        private final Map<String, Boolean> failByToken = new HashMap<>();

        void addRows(String token, List<Map<String, Object>> rows) {
            rowsByToken.put(token, rows);
        }

        void fail(String token) {
            failByToken.put(token, true);
        }

        @Override
        public List<Map<String, Object>> queryForList(String sql, Map<String, ?> params) {
            for (Map.Entry<String, Boolean> entry : failByToken.entrySet()) {
                if (sql.contains(entry.getKey()) && entry.getValue()) {
                    throw new IllegalStateException("forced failure");
                }
            }
            for (Map.Entry<String, List<Map<String, Object>>> entry : rowsByToken.entrySet()) {
                if (sql.contains(entry.getKey())) {
                    return entry.getValue();
                }
            }
            return List.of();
        }
    }
}
