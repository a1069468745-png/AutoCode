package com.autocode.history.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

@ConfigurationProperties(prefix = "autocode.history")
public record HistoryIndexerProperties(
        Path workspaceRoot,
        Path gitCommandPath,
        String defaultBranch,
        int maxCommitWindow
) {
}
