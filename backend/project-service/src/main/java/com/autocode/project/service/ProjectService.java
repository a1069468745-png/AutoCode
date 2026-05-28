package com.autocode.project.service;

import com.autocode.project.cache.ProjectCacheRepository;
import com.autocode.project.domain.ProjectRecord;
import com.autocode.project.domain.ProjectRepository;
import com.autocode.project.web.dto.CreateProjectRequest;
import com.autocode.project.web.dto.CreateProjectResponse;
import com.autocode.project.web.dto.ProjectDetailResponse;
import com.autocode.project.web.dto.ProjectSummaryResponse;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectCacheRepository projectCacheRepository;

    public ProjectService(ProjectRepository projectRepository,
                          ProjectCacheRepository projectCacheRepository) {
        this.projectRepository = projectRepository;
        this.projectCacheRepository = projectCacheRepository;
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

    private ProjectSummaryResponse toSummaryResponse(ProjectRecord projectRecord) {
        return new ProjectSummaryResponse(
                projectRecord.id(),
                projectRecord.name(),
                projectRecord.repoUrl(),
                projectRecord.defaultBranch(),
                projectRecord.status()
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
                projectRecord.status()
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
                projectRecord.createdAt(),
                projectRecord.updatedAt()
        );
    }
}
