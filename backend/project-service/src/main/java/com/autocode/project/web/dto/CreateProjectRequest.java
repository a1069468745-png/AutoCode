package com.autocode.project.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateProjectRequest(
        @NotBlank(message = "name is required")
        @Size(max = 128, message = "name length must be <= 128")
        String name,
        @NotBlank(message = "repoUrl is required")
        @Size(max = 512, message = "repoUrl length must be <= 512")
        String repoUrl,
        @NotBlank(message = "defaultBranch is required")
        @Size(max = 128, message = "defaultBranch length must be <= 128")
        String defaultBranch,
        @Size(max = 256, message = "languageStack length must be <= 256")
        String languageStack,
        @Size(max = 512, message = "docRepoPath length must be <= 512")
        String docRepoPath
) {
}
