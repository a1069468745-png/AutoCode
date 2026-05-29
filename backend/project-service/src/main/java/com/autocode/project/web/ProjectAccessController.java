package com.autocode.project.web;

import com.autocode.project.service.ProjectAccessService;
import com.autocode.project.web.dto.ProjectAccessResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/projects")
public class ProjectAccessController {
    private final ProjectAccessService projectAccessService;

    public ProjectAccessController(ProjectAccessService projectAccessService) {
        this.projectAccessService = projectAccessService;
    }

    @PostMapping("/{projectId}/members/{userId}")
    public ProjectAccessResponse grant(
            @PathVariable("projectId") long projectId,
            @PathVariable("userId") String userId,
            @RequestParam(name = "role", defaultValue = "READER") String role
    ) {
        // Explicit grant endpoint is used by tests and internal tooling before full RBAC UI lands.
        projectAccessService.grant(projectId, userId, role);
        return projectAccessService.check(projectId, userId);
    }

    @GetMapping("/{projectId}/members/{userId}/access")
    public ProjectAccessResponse check(
            @PathVariable("projectId") long projectId,
            @PathVariable("userId") String userId
    ) {
        // Gateway can consume this endpoint to enforce project-level authorization.
        return projectAccessService.check(projectId, userId);
    }
}
