package com.autocode.project.web;

import com.autocode.project.service.ProjectService;
import com.autocode.project.web.dto.CreateProjectRequest;
import com.autocode.project.web.dto.CreateProjectResponse;
import com.autocode.project.web.dto.ProjectDetailResponse;
import com.autocode.project.web.dto.ProjectIndexSyncRequest;
import com.autocode.project.web.dto.ProjectIndexSyncResponse;
import com.autocode.project.web.dto.ProjectSummaryResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    public CreateProjectResponse createProject(@Valid @RequestBody CreateProjectRequest request) {
        return projectService.createProject(request);
    }

    @GetMapping
    public List<ProjectSummaryResponse> listProjects() {
        return projectService.listProjects();
    }

    @GetMapping("/{id}")
    public ProjectDetailResponse getProject(@PathVariable("id") long id) {
        return projectService.getProject(id);
    }

    @PostMapping("/{id}/sync-indexes")
    public ProjectIndexSyncResponse syncIndexes(@PathVariable("id") long id,
                                                @RequestBody(required = false) ProjectIndexSyncRequest request) {
        return projectService.syncIndexes(id, request);
    }
}
