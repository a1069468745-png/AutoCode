package com.autocode.project.web.dto;

public record ProjectSummaryResponse(
        long id,
        String name,
        String repoUrl,
        String defaultBranch,
        String status,
        String indexError
) {
}
