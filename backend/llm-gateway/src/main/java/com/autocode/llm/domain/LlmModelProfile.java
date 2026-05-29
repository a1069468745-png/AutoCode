package com.autocode.llm.domain;

import java.time.Instant;

public record LlmModelProfile(
        long id,
        long projectId,
        String provider,
        String baseUrl,
        String modelName,
        String embeddingModel,
        int timeoutSeconds,
        String fallbackModel,
        boolean enableLocalOnly,
        Instant createdAt,
        Instant updatedAt
) {
}
