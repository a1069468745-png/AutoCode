package com.autocode.history.web;

public record HistoryIndexResponse(
        long projectId,
        String status,
        int commitCount,
        int linkCount
) {}