package com.autocode.llm.web.dto;

import java.time.Instant;

public record ModelProfileResponse(
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
    public static ModelProfileResponse from(com.autocode.llm.domain.LlmModelProfile profile) {
        return new ModelProfileResponse(
                profile.id(),
                profile.projectId(),
                profile.provider(),
                profile.baseUrl(),
                profile.modelName(),
                profile.embeddingModel(),
                profile.timeoutSeconds(),
                profile.fallbackModel(),
                profile.enableLocalOnly(),
                profile.createdAt(),
                profile.updatedAt()
        );
    }
}
