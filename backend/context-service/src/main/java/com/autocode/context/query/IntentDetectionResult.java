package com.autocode.context.query;

public record IntentDetectionResult(
        QueryIntent intent,
        String reason
) {
}
