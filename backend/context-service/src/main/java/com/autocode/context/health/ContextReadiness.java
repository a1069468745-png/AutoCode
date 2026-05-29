package com.autocode.context.health;

public record ContextReadiness(
        String status,
        boolean redisAvailable,
        String detail
) {
}
