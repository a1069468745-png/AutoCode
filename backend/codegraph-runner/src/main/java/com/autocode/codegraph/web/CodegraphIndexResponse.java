package com.autocode.codegraph.web;

public record CodegraphIndexResponse(
        long projectId,
        String status,
        int symbolCount,
        int edgeCount
) {}