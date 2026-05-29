package com.autocode.project.service;

import com.autocode.project.web.dto.ProjectAccessResponse;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ProjectAccessService {
    private final Map<Long, Map<String, String>> roleMappings = new ConcurrentHashMap<>();

    public void grant(long projectId, String userId, String role) {
        // Keep a lightweight in-service mapping for MVP permission checks and gateway integration.
        roleMappings.computeIfAbsent(projectId, ignored -> new ConcurrentHashMap<>())
                .put(userId, normalizeRole(role));
    }

    public ProjectAccessResponse check(long projectId, String userId) {
        String role = roleMappings.getOrDefault(projectId, Map.of()).get(userId);
        boolean allowed = role != null;
        return new ProjectAccessResponse(projectId, userId, role == null ? "NONE" : role, allowed);
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return "READER";
        }
        return role.trim().toUpperCase();
    }
}
