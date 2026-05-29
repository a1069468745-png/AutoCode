package com.autocode.gateway.audit;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix = "autocode.gateway.rate-limit")
public record RateLimitProperties(
        boolean enabled,
        Map<String, RateLimitRule> rules
) {
    public record RateLimitRule(
            int permitsPerSecond,
            int burstSize
    ) {
    }
}
