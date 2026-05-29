package com.autocode.project.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "autocode.project.index")
public record ProjectIndexProperties(
        String defaultWorkspaceRoot,
        String[] excludedDirectories,
        int maxSourceFiles,
        int maxDocuments,
        int maxCommits
) {
    public ProjectIndexProperties {
        excludedDirectories = excludedDirectories == null || excludedDirectories.length == 0
                ? new String[]{".git", "node_modules", "target", "dist", ".worktrees", ".codex-logs"}
                : excludedDirectories;
        maxSourceFiles = maxSourceFiles <= 0 ? 1500 : maxSourceFiles;
        maxDocuments = maxDocuments <= 0 ? 400 : maxDocuments;
        maxCommits = maxCommits <= 0 ? 80 : maxCommits;
    }
}
