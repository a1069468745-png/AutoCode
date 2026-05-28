package com.autocode.codegraph;

import com.autocode.codegraph.config.CodegraphRunnerProperties;
import com.autocode.codegraph.workspace.RunnerWorkspaceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "autocode.codegraph.workspace-root=build/test-workspaces")
class CodegraphRunnerApplicationTest {

    @Autowired
    private RunnerWorkspaceService runnerWorkspaceService;

    @Autowired
    private CodegraphRunnerProperties properties;

    @Test
    void shouldLoadContextWithWorkspaceProperties() {
        assertThat(runnerWorkspaceService).isNotNull();
        assertThat(properties.workspaceRoot()).isEqualTo(Path.of("build/test-workspaces"));
    }
}
