package com.autocode.llm.web.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatCompletionRequest(
        @NotBlank String model,
        @NotBlank String prompt
) {
}
