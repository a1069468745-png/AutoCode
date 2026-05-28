# HI-01 History Indexer Minimal Initialization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the minimum runnable `history-indexer` Spring Boot service skeleton with `autocode.history` configuration, workspace/scope validation, and tests.

**Architecture:** Follow the same initialization depth as `CG-01`: a Spring Boot entrypoint, typed configuration properties, a focused workspace service, and a small immutable scope object for validation. Keep all Git history scanning, diff parsing, and message extraction out of scope so this plan only establishes the safe execution boundary for later `HI-*` tasks.

**Tech Stack:** Java 21, Spring Boot 3.3, Maven, JUnit 5, AssertJ

---

## File Map

- Create: `backend/history-indexer/src/main/java/com/autocode/history/HistoryIndexerApplication.java`
- Create: `backend/history-indexer/src/main/java/com/autocode/history/config/HistoryIndexerProperties.java`
- Create: `backend/history-indexer/src/main/java/com/autocode/history/scope/HistoryScanScope.java`
- Create: `backend/history-indexer/src/main/java/com/autocode/history/workspace/HistoryWorkspaceService.java`
- Create: `backend/history-indexer/src/test/java/com/autocode/history/HistoryIndexerApplicationTest.java`
- Create: `backend/history-indexer/src/test/java/com/autocode/history/scope/HistoryScanScopeTest.java`
- Create: `backend/history-indexer/src/test/java/com/autocode/history/workspace/HistoryWorkspaceServiceTest.java`
- Modify: `backend/history-indexer/pom.xml`
- Modify: `backend/history-indexer/src/main/resources/application.yml`
- Create: `docs/context/progress/2026-05-28-HI-01-History-Indexer工程初始化进度.md`
- Modify: `docs/context/current/02-当前任务状态.md`
- Modify: `docs/indexes/03-当前任务索引.md`
- Modify: `README.md`

### Task 1: Add failing tests for application startup and scope rules

**Files:**
- Create: `backend/history-indexer/src/test/java/com/autocode/history/HistoryIndexerApplicationTest.java`
- Create: `backend/history-indexer/src/test/java/com/autocode/history/scope/HistoryScanScopeTest.java`
- Create: `backend/history-indexer/src/test/java/com/autocode/history/workspace/HistoryWorkspaceServiceTest.java`
- Modify: `backend/history-indexer/pom.xml`

- [ ] **Step 1: Write the failing tests**

```java
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
```

```java
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
```

```java
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
```

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn -pl history-indexer -am test`
Expected: FAIL because `HistoryIndexerApplication`, `HistoryIndexerProperties`, `HistoryScanScope`, and `HistoryWorkspaceService` do not exist yet.

### Task 2: Implement the minimal history-indexer skeleton

**Files:**
- Create: `backend/history-indexer/src/main/java/com/autocode/history/HistoryIndexerApplication.java`
- Create: `backend/history-indexer/src/main/java/com/autocode/history/config/HistoryIndexerProperties.java`
- Create: `backend/history-indexer/src/main/java/com/autocode/history/scope/HistoryScanScope.java`
- Create: `backend/history-indexer/src/main/java/com/autocode/history/workspace/HistoryWorkspaceService.java`
- Modify: `backend/history-indexer/src/main/resources/application.yml`
- Modify: `backend/history-indexer/pom.xml`

- [ ] **Step 1: Write the minimal implementation**

```java
package com.autocode.history;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class HistoryIndexerApplication {

    public static void main(String[] args) {
        SpringApplication.run(HistoryIndexerApplication.class, args);
    }
}
```

```java
package com.autocode.history.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

@ConfigurationProperties(prefix = "autocode.history")
public record HistoryIndexerProperties(
        Path workspaceRoot,
        Path gitCommandPath,
        String defaultBranch,
        int maxCommitWindow
) {
}
```

```java
package com.autocode.history.scope;

import java.util.Optional;

public record HistoryScanScope(
        String branchName,
        Optional<String> fromCommit,
        Optional<String> toCommit,
        int maxCommitWindow
) {

    public static HistoryScanScope of(String branchName, String fromCommit, String toCommit, int maxCommitWindow) {
        if (branchName == null || branchName.isBlank() || branchName.contains("..")) {
            throw new IllegalArgumentException("branchName must not be blank or contain traversal segments");
        }
        if (maxCommitWindow <= 0) {
            throw new IllegalArgumentException("maxCommitWindow must be greater than zero");
        }
        boolean onlyOneCommitProvided = (fromCommit == null) ^ (toCommit == null);
        if (onlyOneCommitProvided) {
            throw new IllegalArgumentException("commit range must provide both fromCommit and toCommit");
        }

        return new HistoryScanScope(
                branchName.trim(),
                Optional.ofNullable(fromCommit),
                Optional.ofNullable(toCommit),
                maxCommitWindow
        );
    }
}
```

```java
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
```

```yaml
autocode:
  history:
    workspace-root: ${WORKSPACE_ROOT:./workspace/history}
    git-command-path: ${GIT_COMMAND_PATH:./tools/git/bin/git}
    default-branch: ${HISTORY_DEFAULT_BRANCH:main}
    max-commit-window: ${HISTORY_MAX_COMMIT_WINDOW:200}
```

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
        </plugin>
    </plugins>
</build>
```

- [ ] **Step 2: Run tests to verify they pass**

Run: `mvn -pl history-indexer -am test`
Expected: PASS with `HistoryIndexerApplicationTest`, `HistoryScanScopeTest`, and `HistoryWorkspaceServiceTest` green.

### Task 3: Sync documentation after verified implementation

**Files:**
- Create: `docs/context/progress/2026-05-28-HI-01-History-Indexer工程初始化进度.md`
- Modify: `docs/context/current/02-当前任务状态.md`
- Modify: `docs/indexes/03-当前任务索引.md`
- Modify: `README.md`

- [ ] **Step 1: Write progress and task state updates**

Document:
- `HI-01` is complete
- The module now has a Spring Boot entrypoint, typed `autocode.history` configuration, a workspace preparation service, and a minimal scan scope validator
- Verification command used: `mvn -pl history-indexer -am test`
- Next task becomes `KI-01`

- [ ] **Step 2: Re-run verification before final status**

Run: `mvn -pl history-indexer -am test`
Expected: PASS again, confirming docs were the only remaining edits.
