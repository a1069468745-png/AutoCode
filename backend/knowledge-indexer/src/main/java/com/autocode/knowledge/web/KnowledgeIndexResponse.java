package com.autocode.knowledge.web;

public record KnowledgeIndexResponse(
        long projectId,
        String status,
        int documentCount,
        int requirementCount,
        int linkCount
) {}