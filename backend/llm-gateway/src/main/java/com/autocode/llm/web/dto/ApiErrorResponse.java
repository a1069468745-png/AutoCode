package com.autocode.llm.web.dto;

import java.time.Instant;

public record ApiErrorResponse(
        String code,
        String message,
        String path,
        Instant timestamp
) {
}
