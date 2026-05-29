package com.autocode.project.web.dto;

public record ProjectIndexSyncRequest(
        String workspaceRoot,
        Integer maxCommits
) {
}
