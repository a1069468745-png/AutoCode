package com.autocode.knowledge.web;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record KnowledgeIndexRequest(
        @NotNull @Min(1) Long projectId,
        @NotBlank String workspaceRoot,
        String docRepoPath
) {}