package com.autocode.gateway.security;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ProjectAccessEvaluator {
    private final Map<Long, Set<String>> allowedUsersByProject = new ConcurrentHashMap<>();

    public ProjectAccessEvaluator() {
        // Seed a minimal default mapping for local development and integration tests.
        grant(1L, "dev-user");
    }

    public void grant(long projectId, String userId) {
        allowedUsersByProject.computeIfAbsent(projectId, ignored -> ConcurrentHashMap.newKeySet()).add(userId);
    }

    public boolean isAllowed(long projectId, String userId) {
        return allowedUsersByProject.getOrDefault(projectId, Set.of()).contains(userId);
    }
}
