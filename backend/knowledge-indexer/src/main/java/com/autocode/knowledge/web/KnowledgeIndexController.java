package com.autocode.knowledge.web;

import com.autocode.knowledge.index.KnowledgeIndexService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;

@RestController
@RequestMapping("/api/runner/knowledge")
public class KnowledgeIndexController {

    private final KnowledgeIndexService knowledgeIndexService;

    public KnowledgeIndexController(KnowledgeIndexService knowledgeIndexService) {
        this.knowledgeIndexService = knowledgeIndexService;
    }

    @PostMapping("/index")
    public KnowledgeIndexResponse index(@Valid @RequestBody KnowledgeIndexRequest request) {
        Path workspaceRoot = Path.of(request.workspaceRoot()).toAbsolutePath().normalize();
        var result = knowledgeIndexService.index(request.projectId(), workspaceRoot, request.docRepoPath());
        return new KnowledgeIndexResponse(
                request.projectId(),
                "COMPLETED",
                result.documentCount(),
                result.requirementCount(),
                result.linkCount()
        );
    }
}