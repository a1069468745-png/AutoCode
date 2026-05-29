package com.autocode.llm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "autocode.llm")
public record LlmGatewayProperties(
        String provider,
        String baseUrl,
        String apiKey,
        String modelName,
        String fallbackModel,
        int timeoutSeconds,
        boolean enableLocalOnly
) {
}
