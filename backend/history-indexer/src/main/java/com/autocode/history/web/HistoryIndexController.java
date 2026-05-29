package com.autocode.history.web;

import com.autocode.history.index.HistoryIndexService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;

@RestController
@RequestMapping("/api/runner/history")
public class HistoryIndexController {

    private final HistoryIndexService historyIndexService;

    public HistoryIndexController(HistoryIndexService historyIndexService) {
        this.historyIndexService = historyIndexService;
    }

    @PostMapping("/index")
    public HistoryIndexResponse index(@Valid @RequestBody HistoryIndexRequest request) {
        Path workspaceRoot = Path.of(request.workspaceRoot()).toAbsolutePath().normalize();
        var result = historyIndexService.index(request.projectId(), workspaceRoot, request.maxCommits());
        return new HistoryIndexResponse(
                request.projectId(),
                "COMPLETED",
                result.commitCount(),
                result.linkCount()
        );
    }
}