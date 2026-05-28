package com.autocode.history.workspace;

import com.autocode.history.config.HistoryIndexerProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HistoryWorkspaceServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldPrepareWorkspaceInsideConfiguredRoot() {
        HistoryWorkspaceService service = new HistoryWorkspaceService(
                new HistoryIndexerProperties(tempDir, Path.of("tools/git/bin/git"), "main", 200)
        );

        Path workspace = service.prepareWorkspace("project-beta", "feature/history");

        assertThat(workspace).isEqualTo(tempDir.resolve("project-beta").resolve("feature_history").toAbsolutePath().normalize());
        assertThat(workspace).exists().isDirectory();
    }

    @Test
    void shouldRejectWorkspaceTraversalSegments() {
        HistoryWorkspaceService service = new HistoryWorkspaceService(
                new HistoryIndexerProperties(tempDir, Path.of("tools/git/bin/git"), "main", 200)
        );

        assertThatThrownBy(() -> service.prepareWorkspace("../escape", "main"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("projectKey");

        assertThatThrownBy(() -> service.prepareWorkspace("project-beta", "../../escape"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("branchName");
    }
}
