package com.autocode.context.query;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Component
public class QueryIntentResolver {
    private static final QueryIntent DEFAULT_FALLBACK_INTENT = QueryIntent.CODE_LOCATE;

    // Use insertion order as matching priority to resolve ambiguous natural-language queries.
    private static final Map<QueryIntent, String[]> INTENT_KEYWORDS = new LinkedHashMap<>();

    static {
        INTENT_KEYWORDS.put(QueryIntent.CALL_RELATION, new String[]{
                "调用", "调用链", "谁调用", "call", "caller", "callee", "invoke", "dependency"
        });
        INTENT_KEYWORDS.put(QueryIntent.HISTORY_TRACE, new String[]{
                "历史", "提交", "谁改", "最近修改", "变更", "commit", "history", "blame", "when"
        });
        INTENT_KEYWORDS.put(QueryIntent.REQUIREMENT_ANALYSIS, new String[]{
                "新需求", "改造", "影响范围", "风险点", "测试建议", "analysis", "impact", "plan"
        });
        INTENT_KEYWORDS.put(QueryIntent.DOCUMENT_TRACE, new String[]{
                "文档", "需求", "设计", "复盘", "说明", "markdown", "doc", "spec", "requirement"
        });
        INTENT_KEYWORDS.put(QueryIntent.CODE_LOCATE, new String[]{
                "代码", "函数", "类", "文件", "在哪", "定位", "symbol", "method", "class", "where"
        });
    }

    public IntentDetectionResult resolve(ContextQueryRequest request) {
        if (request == null) {
            return fallback("request is null");
        }
        // Explicit client intent always wins over keyword inference.
        if (request.preferredIntent() != null && request.preferredIntent() != QueryIntent.UNKNOWN) {
            return new IntentDetectionResult(request.preferredIntent(), "preferred intent from request");
        }
        String normalizedText = normalize(request.queryText());
        if (normalizedText.isEmpty()) {
            return fallback("query text is empty");
        }
        // First-match policy keeps routing deterministic and easy to debug.
        for (Map.Entry<QueryIntent, String[]> entry : INTENT_KEYWORDS.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (normalizedText.contains(keyword)) {
                    return new IntentDetectionResult(entry.getKey(), "matched keyword: " + keyword);
                }
            }
        }
        return fallback("no keyword matched");
    }

    private IntentDetectionResult fallback(String reason) {
        return new IntentDetectionResult(DEFAULT_FALLBACK_INTENT, "fallback: " + reason);
    }

    private String normalize(String queryText) {
        if (queryText == null) {
            return "";
        }
        return queryText.trim().toLowerCase(Locale.ROOT);
    }
}
