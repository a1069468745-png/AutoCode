package com.autocode.project.domain;

import java.time.Instant;

public record ProjectRecord(
        long id,
        String name,
        String repoUrl,
        String defaultBranch,
        String languageStack,
        String docRepoPath,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
