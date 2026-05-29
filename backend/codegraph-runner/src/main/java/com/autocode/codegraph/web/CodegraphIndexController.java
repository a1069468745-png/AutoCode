package com.autocode.codegraph.web;

import com.autocode.codegraph.index.CodegraphIndexService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;

@RestController
@RequestMapping("/api/runner/codegraph")
public class CodegraphIndexController {

    private final CodegraphIndexService codegraphIndexService;

    public CodegraphIndexController(CodegraphIndexService codegraphIndexService) {
        this.codegraphIndexService = codegraphIndexService;
    }

    @PostMapping("/index")
    public CodegraphIndexResponse index(@Valid @RequestBody CodegraphIndexRequest request) {
        Path workspaceRoot = Path.of(request.workspaceRoot()).toAbsolutePath().normalize();
        var result = codegraphIndexService.index(request.projectId(), workspaceRoot);
        return new CodegraphIndexResponse(
                request.projectId(),
                "COMPLETED",
                result.symbolCount(),
                result.edgeCount()
        );
    }
}