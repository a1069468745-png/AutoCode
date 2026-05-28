package com.autocode.history.workspace;

import com.autocode.history.config.HistoryIndexerProperties;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class HistoryWorkspaceService {

    private final HistoryIndexerProperties properties;

    public HistoryWorkspaceService(HistoryIndexerProperties properties) {
        this.properties = properties;
    }

    public Path prepareWorkspace(String projectKey, String branchName) {
        String safeProjectKey = validateSegment(projectKey, "projectKey");
        String safeBranchName = validateSegment(branchName, "branchName")
                .replace('/', '_')
                .replace('\\', '_');

        Path root = properties.workspaceRoot().toAbsolutePath().normalize();
        Path workspace = root.resolve(safeProjectKey).resolve(safeBranchName).normalize();
        if (!workspace.startsWith(root)) {
            throw new IllegalArgumentException("Workspace path escapes configured root");
        }

        try {
            Files.createDirectories(workspace);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to prepare history workspace directory", ex);
        }

        return workspace;
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
