package com.autocode.codegraph.workspace;

import com.autocode.codegraph.config.CodegraphRunnerProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RunnerWorkspaceServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldResolveWorkspaceInsideConfiguredRoot() {
        RunnerWorkspaceService service = new RunnerWorkspaceService(
                new CodegraphRunnerProperties(tempDir, Path.of("tools/codegraph"), 300)
        );

        Path workspace = service.prepareWorkspace("project-alpha", "feature/demo");

        assertThat(workspace).isEqualTo(tempDir.resolve("project-alpha").resolve("feature_demo"));
        assertThat(workspace).exists().isDirectory();
    }

    @Test
    void shouldRejectWorkspaceTraversalSegments() {
        RunnerWorkspaceService service = new RunnerWorkspaceService(
                new CodegraphRunnerProperties(tempDir, Path.of("tools/codegraph"), 300)
        );

        assertThatThrownBy(() -> service.prepareWorkspace("../escape", "main"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("projectKey");

        assertThatThrownBy(() -> service.prepareWorkspace("project-alpha", "../../escape"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("branchName");
    }
}
