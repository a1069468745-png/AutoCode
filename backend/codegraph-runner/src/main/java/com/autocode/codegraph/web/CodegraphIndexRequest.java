package com.autocode.codegraph.web;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CodegraphIndexRequest(
        @NotNull @Min(1) Long projectId,
        @NotBlank String workspaceRoot
) {}