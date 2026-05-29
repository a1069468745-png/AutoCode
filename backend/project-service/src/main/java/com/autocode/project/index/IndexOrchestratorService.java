package com.autocode.project.index;

import com.autocode.project.cache.ProjectCacheRepository;
import com.autocode.project.domain.ProjectRecord;
import com.autocode.project.domain.ProjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class IndexOrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(IndexOrchestratorService.class);

    private final ProjectRepository projectRepository;
    private final ProjectCacheRepository projectCacheRepository;
    private final ProjectIndexSyncService projectIndexSyncService;

    public IndexOrchestratorService(ProjectRepository projectRepository,
                                    ProjectCacheRepository projectCacheRepository,
                                    ProjectIndexSyncService projectIndexSyncService) {
        this.projectRepository = projectRepository;
        this.projectCacheRepository = projectCacheRepository;
        this.projectIndexSyncService = projectIndexSyncService;
    }

    @Async
    public void triggerAsyncIndexing(ProjectRecord project, String workspaceRoot, Integer maxCommits) {
        long projectId = project.id();
        log.info("Starting async index for project {} ({})", projectId, project.name());

        projectRepository.updateStatus(projectId, "INDEXING");
        projectCacheRepository.evictProjectList();
        projectCacheRepository.evictProjectDetail(projectId);

        try {
            var summary = projectIndexSyncService.sync(project, workspaceRoot, maxCommits);
            projectRepository.updateStatus(projectId, "READY");
            log.info("Async index completed for project {}: symbols={}, edges={}, commits={}, docs={}",
                    projectId, summary.symbolCount(), summary.edgeCount(),
                    summary.commitCount(), summary.documentCount());
        } catch (RuntimeException exception) {
            String errorMessage = exception.getMessage() != null
                    ? exception.getMessage()
                    : exception.getClass().getSimpleName();
            String truncated = errorMessage.length() > 2000 ? errorMessage.substring(0, 2000) : errorMessage;
            projectRepository.updateStatusWithError(projectId, "FAILED", truncated);
            log.error("Async index failed for project {}: {}", projectId, truncated, exception);
        } finally {
            projectCacheRepository.evictProjectList();
            projectCacheRepository.evictProjectDetail(projectId);
        }
    }
}
