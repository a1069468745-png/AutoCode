package com.autocode.history.web;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record HistoryIndexRequest(
        @NotNull @Min(1) Long projectId,
        @NotBlank String workspaceRoot,
        Integer maxCommits
) {}