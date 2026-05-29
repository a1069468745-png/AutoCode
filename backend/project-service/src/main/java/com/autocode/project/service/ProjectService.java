package com.autocode.project.service;

import com.autocode.project.cache.ProjectCacheRepository;
import com.autocode.project.domain.ProjectRecord;
import com.autocode.project.domain.ProjectRepository;
import com.autocode.project.index.IndexOrchestratorService;
import com.autocode.project.index.ProjectIndexSyncService;
import com.autocode.project.web.dto.CreateProjectRequest;
import com.autocode.project.web.dto.CreateProjectResponse;
import com.autocode.project.web.dto.ProjectDetailResponse;
import com.autocode.project.web.dto.ProjectIndexSyncRequest;
import com.autocode.project.web.dto.ProjectIndexSyncResponse;
import com.autocode.project.web.dto.ProjectSummaryResponse;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectCacheRepository projectCacheRepository;
    private final ProjectIndexSyncService projectIndexSyncService;
    private final IndexOrchestratorService indexOrchestratorService;

    public ProjectService(ProjectRepository projectRepository,
                          ProjectCacheRepository projectCacheRepository,
                          ProjectIndexSyncService projectIndexSyncService,
                          IndexOrchestratorService indexOrchestratorService) {
        this.projectRepository = projectRepository;
        this.projectCacheRepository = projectCacheRepository;
        this.projectIndexSyncService = projectIndexSyncService;
        this.indexOrchestratorService = indexOrchestratorService;
    }

    public CreateProjectResponse createProject(CreateProjectRequest request) {
        try {
            ProjectRecord projectRecord = projectRepository.create(
                    request.name(),
                    request.repoUrl(),
                    request.defaultBranch(),
                    request.languageStack(),
                    request.docRepoPath()
            );
            projectCacheRepository.evictProjectList();
            return toCreateResponse(projectRecord);
        } catch (DuplicateKeyException exception) {
            throw new ProjectNameConflictException(request.name(), exception);
        }
    }

    public List<ProjectSummaryResponse> listProjects() {
        return projectCacheRepository.findProjectList()
                .orElseGet(() -> {
                    List<ProjectSummaryResponse> projects = projectRepository.findAll().stream()
                            .map(this::toSummaryResponse)
                            .toList();
                    projectCacheRepository.cacheProjectList(projects);
                    return projects;
                });
    }

    public ProjectDetailResponse getProject(long projectId) {
        return projectCacheRepository.findProjectDetail(projectId)
                .orElseGet(() -> {
                    ProjectRecord projectRecord = projectRepository.findById(projectId)
                            .orElseThrow(() -> new ProjectNotFoundException(projectId));
                    ProjectDetailResponse projectDetail = toDetailResponse(projectRecord);
                    projectCacheRepository.cacheProjectDetail(projectDetail);
                    return projectDetail;
                });
    }

    public ProjectIndexSyncResponse syncIndexes(long projectId, ProjectIndexSyncRequest request) {
        ProjectRecord projectRecord = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        indexOrchestratorService.triggerAsyncIndexing(
                projectRecord,
                request == null ? null : request.workspaceRoot(),
                request == null ? null : request.maxCommits()
        );

        return new ProjectIndexSyncResponse(
                projectId,
                "INDEXING",
                request != null && request.workspaceRoot() != null ? request.workspaceRoot() : "<auto>",
                0, 0, 0, 0, 0, 0
        );
    }

    private ProjectSummaryResponse toSummaryResponse(ProjectRecord projectRecord) {
        return new ProjectSummaryResponse(
                projectRecord.id(),
                projectRecord.name(),
                projectRecord.repoUrl(),
                projectRecord.defaultBranch(),
                projectRecord.status(),
                projectRecord.indexError()
        );
    }

    private CreateProjectResponse toCreateResponse(ProjectRecord projectRecord) {
        return new CreateProjectResponse(
                projectRecord.id(),
                projectRecord.name(),
                projectRecord.repoUrl(),
                projectRecord.defaultBranch(),
                projectRecord.languageStack(),
                projectRecord.docRepoPath(),
                projectRecord.status(),
                projectRecord.indexError()
        );
    }

    private ProjectDetailResponse toDetailResponse(ProjectRecord projectRecord) {
        return new ProjectDetailResponse(
                projectRecord.id(),
                projectRecord.name(),
                projectRecord.repoUrl(),
                projectRecord.defaultBranch(),
                projectRecord.languageStack(),
                projectRecord.docRepoPath(),
                projectRecord.status(),
                projectRecord.indexError(),
                projectRecord.createdAt(),
                projectRecord.updatedAt()
        );
    }
}
