package com.autocode.codegraph.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

@ConfigurationProperties(prefix = "autocode.codegraph")
public record CodegraphRunnerProperties(
        Path workspaceRoot,
        Path commandPath,
        int commandTimeoutSeconds
) {
}
