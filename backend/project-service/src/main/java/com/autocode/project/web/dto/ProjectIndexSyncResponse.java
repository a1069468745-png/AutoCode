package com.autocode.project.web.dto;

public record ProjectIndexSyncResponse(
        long projectId,
        String status,
        String workspaceRoot,
        int symbolCount,
        int edgeCount,
        int commitCount,
        int documentCount,
        int requirementCount,
        int linkCount
) {
}
