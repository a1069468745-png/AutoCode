package com.autocode.project.web.dto;

public record CreateProjectResponse(
        long id,
        String name,
        String repoUrl,
        String defaultBranch,
        String languageStack,
        String docRepoPath,
        String status,
        String indexError
) {
}
