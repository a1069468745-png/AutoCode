package com.autocode.llm.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpsertModelProfileRequest(
        @NotBlank @Size(max = 64) String provider,
        @Size(max = 512) String baseUrl,
        @NotBlank @Size(max = 128) String modelName,
        @Size(max = 128) String embeddingModel,
        @Min(1) int timeoutSeconds,
        @Size(max = 128) String fallbackModel,
        boolean enableLocalOnly
) {
}
