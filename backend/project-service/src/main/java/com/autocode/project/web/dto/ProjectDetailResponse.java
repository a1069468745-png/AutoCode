package com.autocode.project.web.dto;

import java.time.Instant;

public record ProjectDetailResponse(
        long id,
        String name,
        String repoUrl,
        String defaultBranch,
        String languageStack,
        String docRepoPath,
        String status,
        String indexError,
        Instant createdAt,
        Instant updatedAt
) {
}
