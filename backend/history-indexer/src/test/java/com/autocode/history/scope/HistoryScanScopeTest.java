package com.autocode.history.scope;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HistoryScanScopeTest {

    @Test
    void shouldCreateScopeWithCompleteCommitRange() {
        HistoryScanScope scope = HistoryScanScope.of("main", "abc123", "def456", 200);

        assertThat(scope.branchName()).isEqualTo("main");
        assertThat(scope.fromCommit()).hasValue("abc123");
        assertThat(scope.toCommit()).hasValue("def456");
        assertThat(scope.maxCommitWindow()).isEqualTo(200);
    }

    @Test
    void shouldRejectIncompleteCommitRange() {
        assertThatThrownBy(() -> HistoryScanScope.of("main", "abc123", null, 200))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("commit range");
    }

    @Test
    void shouldRejectNonPositiveCommitWindow() {
        assertThatThrownBy(() -> HistoryScanScope.of("main", null, null, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxCommitWindow");
    }
}
