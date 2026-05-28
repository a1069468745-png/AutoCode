package com.autocode.project.config;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "autocode.project.cache")
public record ProjectCacheProperties(
        @NotNull Duration detailTtl,
        @NotNull Duration listTtl
) {
}
