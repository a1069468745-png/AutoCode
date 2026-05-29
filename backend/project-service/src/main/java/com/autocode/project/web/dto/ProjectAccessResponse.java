package com.autocode.project.web.dto;

public record ProjectAccessResponse(
        long projectId,
        String userId,
        String role,
        boolean allowed
) {
}
