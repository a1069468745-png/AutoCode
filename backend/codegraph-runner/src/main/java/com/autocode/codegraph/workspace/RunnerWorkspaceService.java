package com.autocode.codegraph.workspace;

import com.autocode.codegraph.config.CodegraphRunnerProperties;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class RunnerWorkspaceService {

    private final CodegraphRunnerProperties properties;

    public RunnerWorkspaceService(CodegraphRunnerProperties properties) {
        this.properties = properties;
    }

    public Path prepareWorkspace(String projectKey, String branchName) {
        String safeProjectKey = validateSegment(projectKey, "projectKey");
        String safeBranchName = validateSegment(branchName, "branchName")
                .replace('/', '_')
                .replace('\\', '_');

        Path workspace = properties.workspaceRoot()
                .resolve(safeProjectKey)
                .resolve(safeBranchName)
                .normalize();

        Path root = properties.workspaceRoot().toAbsolutePath().normalize();
        Path absoluteWorkspace = workspace.toAbsolutePath().normalize();
        if (!absoluteWorkspace.startsWith(root)) {
            throw new IllegalArgumentException("Workspace path escapes configured root");
        }

        try {
            Files.createDirectories(absoluteWorkspace);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to prepare workspace directory", ex);
        }

        return absoluteWorkspace;
    }

    private String validateSegment(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        if (value.contains("..")) {
            throw new IllegalArgumentException(fieldName + " must not contain traversal segments");
        }
        return value.trim();
    }
}
