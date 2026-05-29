package com.autocode.context.query;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QueryIntentResolverTest {
    private final QueryIntentResolver resolver = new QueryIntentResolver();

    @Test
    void shouldReturnPreferredIntentWhenProvided() {
        ContextQueryRequest request = new ContextQueryRequest(
                1L,
                "随便什么问题",
                QueryIntent.HISTORY_TRACE,
                null
        );

        IntentDetectionResult result = resolver.resolve(request);

        assertThat(result.intent()).isEqualTo(QueryIntent.HISTORY_TRACE);
    }

    @Test
    void shouldRecognizeCallRelationIntent() {
        ContextQueryRequest request = new ContextQueryRequest(1L, "谁调用了这个方法", null, null);

        IntentDetectionResult result = resolver.resolve(request);

        assertThat(result.intent()).isEqualTo(QueryIntent.CALL_RELATION);
    }

    @Test
    void shouldRecognizeHistoryTraceIntent() {
        ContextQueryRequest request = new ContextQueryRequest(1L, "最近是谁改了这个文件", null, null);

        IntentDetectionResult result = resolver.resolve(request);

        assertThat(result.intent()).isEqualTo(QueryIntent.HISTORY_TRACE);
    }

    @Test
    void shouldRecognizeDocumentTraceIntent() {
        ContextQueryRequest request = new ContextQueryRequest(1L, "这个需求对应的设计文档在哪", null, null);

        IntentDetectionResult result = resolver.resolve(request);

        assertThat(result.intent()).isEqualTo(QueryIntent.DOCUMENT_TRACE);
    }

    @Test
    void shouldRecognizeRequirementAnalysisIntent() {
        ContextQueryRequest request = new ContextQueryRequest(1L, "新需求会影响哪些模块", null, null);

        IntentDetectionResult result = resolver.resolve(request);

        assertThat(result.intent()).isEqualTo(QueryIntent.REQUIREMENT_ANALYSIS);
    }

    @Test
    void shouldRecognizeCodeLocateIntent() {
        ContextQueryRequest request = new ContextQueryRequest(1L, "这个类在哪个文件", null, null);

        IntentDetectionResult result = resolver.resolve(request);

        assertThat(result.intent()).isEqualTo(QueryIntent.CODE_LOCATE);
    }

    @Test
    void shouldFallbackWhenTextIsEmpty() {
        ContextQueryRequest request = new ContextQueryRequest(1L, "   ", null, null);

        IntentDetectionResult result = resolver.resolve(request);

        assertThat(result.intent()).isEqualTo(QueryIntent.CODE_LOCATE);
        assertThat(result.reason()).contains("fallback");
    }

    @Test
    void shouldFallbackWhenNothingMatches() {
        ContextQueryRequest request = new ContextQueryRequest(1L, "天气怎么样", null, null);

        IntentDetectionResult result = resolver.resolve(request);

        assertThat(result.intent()).isEqualTo(QueryIntent.CODE_LOCATE);
        assertThat(result.reason()).contains("fallback");
    }
}
