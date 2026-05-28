package com.autocode.history;

import com.autocode.history.config.HistoryIndexerProperties;
import com.autocode.history.workspace.HistoryWorkspaceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "autocode.history.workspace-root=build/test-history-workspaces")
class HistoryIndexerApplicationTest {

    @Autowired
    private HistoryWorkspaceService historyWorkspaceService;

    @Autowired
    private HistoryIndexerProperties properties;

    @Test
    void shouldLoadContextWithHistoryProperties() {
        assertThat(historyWorkspaceService).isNotNull();
        assertThat(properties.workspaceRoot()).isEqualTo(Path.of("build/test-history-workspaces"));
    }
}
