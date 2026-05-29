# KI-01 Knowledge Indexer Minimal Initialization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the minimum runnable `knowledge-indexer` Spring Boot service skeleton with `autocode.knowledge` configuration, controlled docs-root handling, Markdown/frontmatter parsing, and tests.

**Architecture:** Keep the module at the same initialization depth as `CG-01` and `HI-01`: one Spring Boot entrypoint, one typed properties object, one focused docs-root service, and one narrow parser for Markdown plus YAML frontmatter. Leave all recursive scanning, database persistence, frontmatter schema validation, and document-link construction out of scope so `KI-01` only establishes a safe and testable boundary for later `KI-*` tasks.

**Tech Stack:** Java 21, Spring Boot 3.3, Maven, SnakeYAML, JUnit 5, AssertJ

---

## File Map

- Create: `backend/knowledge-indexer/src/main/java/com/autocode/knowledge/KnowledgeIndexerApplication.java`
- Create: `backend/knowledge-indexer/src/main/java/com/autocode/knowledge/config/KnowledgeIndexerProperties.java`
- Create: `backend/knowledge-indexer/src/main/java/com/autocode/knowledge/workspace/KnowledgeWorkspaceService.java`
- Create: `backend/knowledge-indexer/src/main/java/com/autocode/knowledge/parse/ParsedMarkdownDocument.java`
- Create: `backend/knowledge-indexer/src/main/java/com/autocode/knowledge/parse/MarkdownDocumentParser.java`
- Create: `backend/knowledge-indexer/src/main/java/com/autocode/knowledge/parse/MarkdownParserConfiguration.java`
- Create: `backend/knowledge-indexer/src/test/java/com/autocode/knowledge/KnowledgeIndexerApplicationTest.java`
- Create: `backend/knowledge-indexer/src/test/java/com/autocode/knowledge/workspace/KnowledgeWorkspaceServiceTest.java`
- Create: `backend/knowledge-indexer/src/test/java/com/autocode/knowledge/parse/MarkdownDocumentParserTest.java`
- Modify: `backend/knowledge-indexer/pom.xml`
- Modify: `backend/knowledge-indexer/src/main/resources/application.yml`
- Create: `docs/context/progress/2026-05-28-KI-01-Knowledge-Indexer工程初始化进度.md`
- Modify: `docs/context/current/02-当前任务状态.md`
- Modify: `docs/indexes/03-当前任务索引.md`
- Modify: `README.md`

### Task 1: Add failing tests for startup, docs-root safety, and Markdown parsing

**Files:**
- Modify: `backend/knowledge-indexer/pom.xml`
- Create: `backend/knowledge-indexer/src/test/java/com/autocode/knowledge/KnowledgeIndexerApplicationTest.java`
- Create: `backend/knowledge-indexer/src/test/java/com/autocode/knowledge/workspace/KnowledgeWorkspaceServiceTest.java`
- Create: `backend/knowledge-indexer/src/test/java/com/autocode/knowledge/parse/MarkdownDocumentParserTest.java`

- [ ] **Step 1: Add the test dependencies**

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
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

- [ ] **Step 2: Write the failing application context test**

```java
package com.autocode.knowledge;

import com.autocode.knowledge.config.KnowledgeIndexerProperties;
import com.autocode.knowledge.workspace.KnowledgeWorkspaceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "autocode.knowledge.docs-root=build/test-knowledge-docs",
        "autocode.knowledge.markdown-extensions=.md,.mdx",
        "autocode.knowledge.frontmatter-delimiter=---"
})
class KnowledgeIndexerApplicationTest {

    @Autowired
    private KnowledgeWorkspaceService knowledgeWorkspaceService;

    @Autowired
    private KnowledgeIndexerProperties properties;

    @Test
    void shouldLoadContextWithKnowledgeProperties() {
        assertThat(knowledgeWorkspaceService).isNotNull();
        assertThat(properties.docsRoot()).isEqualTo(Path.of("build/test-knowledge-docs"));
        assertThat(properties.markdownExtensions()).containsExactly(".md", ".mdx");
        assertThat(properties.frontmatterDelimiter()).isEqualTo("---");
    }
}
```

- [ ] **Step 3: Write the failing docs-root safety tests**

```java
package com.autocode.knowledge.workspace;

import com.autocode.knowledge.config.KnowledgeIndexerProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KnowledgeWorkspaceServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldResolveDocumentInsideDocsRoot() throws Exception {
        Path docsRoot = Files.createDirectories(tempDir.resolve("vault"));
        Files.createDirectories(docsRoot.resolve("specs"));
        Files.writeString(docsRoot.resolve("specs/ki-01.md"), "# title");

        KnowledgeWorkspaceService service = new KnowledgeWorkspaceService(
                new KnowledgeIndexerProperties(docsRoot, List.of(".md", ".mdx"), "---")
        );

        Path resolved = service.resolveDocument("specs/ki-01.md");

        assertThat(resolved).isEqualTo(docsRoot.resolve("specs/ki-01.md").toAbsolutePath().normalize());
    }

    @Test
    void shouldRejectTraversalOutsideDocsRoot() throws Exception {
        Path docsRoot = Files.createDirectories(tempDir.resolve("vault"));

        KnowledgeWorkspaceService service = new KnowledgeWorkspaceService(
                new KnowledgeIndexerProperties(docsRoot, List.of(".md"), "---")
        );

        assertThatThrownBy(() -> service.resolveDocument("../escape.md"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("documentPath");
    }
}
```

- [ ] **Step 4: Write the failing Markdown/frontmatter parser tests**

```java
package com.autocode.knowledge.parse;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MarkdownDocumentParserTest {

    @Test
    void shouldParseMarkdownWithFrontmatter() {
        MarkdownDocumentParser parser = new MarkdownDocumentParser("---");

        ParsedMarkdownDocument document = parser.parse("""
                ---
                title: Knowledge Indexer
                module: KI
                ---
                # Heading

                body
                """);

        assertThat(document.hasFrontmatter()).isTrue();
        assertThat(document.frontmatter()).containsEntry("title", "Knowledge Indexer");
        assertThat(document.frontmatter()).containsEntry("module", "KI");
        assertThat(document.body()).contains("# Heading");
    }

    @Test
    void shouldParseMarkdownWithoutFrontmatter() {
        MarkdownDocumentParser parser = new MarkdownDocumentParser("---");

        ParsedMarkdownDocument document = parser.parse("""
                # Heading

                plain body
                """);

        assertThat(document.hasFrontmatter()).isFalse();
        assertThat(document.frontmatter()).isEmpty();
        assertThat(document.body()).contains("plain body");
    }

    @Test
    void shouldRejectInvalidFrontmatter() {
        MarkdownDocumentParser parser = new MarkdownDocumentParser("---");

        assertThatThrownBy(() -> parser.parse("""
                ---
                title: [broken
                ---
                body
                """))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("frontmatter");
    }
}
```

- [ ] **Step 5: Run tests to verify they fail**

Run: `mvn -pl knowledge-indexer -am test`

Expected: FAIL because `KnowledgeIndexerApplication`, `KnowledgeIndexerProperties`, `KnowledgeWorkspaceService`, `MarkdownDocumentParser`, and `ParsedMarkdownDocument` do not exist yet.

### Task 2: Implement the minimal knowledge-indexer skeleton

**Files:**
- Create: `backend/knowledge-indexer/src/main/java/com/autocode/knowledge/KnowledgeIndexerApplication.java`
- Create: `backend/knowledge-indexer/src/main/java/com/autocode/knowledge/config/KnowledgeIndexerProperties.java`
- Create: `backend/knowledge-indexer/src/main/java/com/autocode/knowledge/workspace/KnowledgeWorkspaceService.java`
- Create: `backend/knowledge-indexer/src/main/java/com/autocode/knowledge/parse/ParsedMarkdownDocument.java`
- Create: `backend/knowledge-indexer/src/main/java/com/autocode/knowledge/parse/MarkdownDocumentParser.java`
- Create: `backend/knowledge-indexer/src/main/java/com/autocode/knowledge/parse/MarkdownParserConfiguration.java`
- Modify: `backend/knowledge-indexer/src/main/resources/application.yml`
- Modify: `backend/knowledge-indexer/pom.xml`

- [ ] **Step 1: Add the runtime dependency for YAML parsing**

```xml
<dependency>
    <groupId>org.yaml</groupId>
    <artifactId>snakeyaml</artifactId>
</dependency>
```

- [ ] **Step 2: Create the Spring Boot entrypoint**

```java
package com.autocode.knowledge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class KnowledgeIndexerApplication {

    public static void main(String[] args) {
        SpringApplication.run(KnowledgeIndexerApplication.class, args);
    }
}
```

- [ ] **Step 3: Create the typed configuration properties**

```java
package com.autocode.knowledge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;
import java.util.List;

@ConfigurationProperties(prefix = "autocode.knowledge")
public record KnowledgeIndexerProperties(
        Path docsRoot,
        List<String> markdownExtensions,
        String frontmatterDelimiter
) {
}
```

- [ ] **Step 4: Create the docs-root boundary service**

```java
package com.autocode.knowledge.workspace;

import com.autocode.knowledge.config.KnowledgeIndexerProperties;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class KnowledgeWorkspaceService {

    private final Path docsRoot;

    public KnowledgeWorkspaceService(KnowledgeIndexerProperties properties) {
        if (properties.docsRoot() == null) {
            throw new IllegalArgumentException("docsRoot must not be null");
        }
        this.docsRoot = properties.docsRoot().toAbsolutePath().normalize();
    }

    public Path docsRoot() {
        return docsRoot;
    }

    public Path prepareDocsRoot() {
        try {
            return Files.createDirectories(docsRoot);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to prepare docs root: " + docsRoot, exception);
        }
    }

    public Path resolveDocument(String documentPath) {
        if (documentPath == null || documentPath.isBlank() || documentPath.contains("..")) {
            throw new IllegalArgumentException("documentPath must not be blank or contain traversal segments");
        }

        Path resolved = docsRoot.resolve(documentPath).normalize().toAbsolutePath();
        if (!resolved.startsWith(docsRoot)) {
            throw new IllegalArgumentException("documentPath must stay inside docs root");
        }
        return resolved;
    }
}
```

- [ ] **Step 5: Create the parsed document result object**

```java
package com.autocode.knowledge.parse;

import java.util.Map;

public record ParsedMarkdownDocument(
        boolean hasFrontmatter,
        Map<String, Object> frontmatter,
        String body
) {
}
```

- [ ] **Step 6: Create the Markdown/frontmatter parser**

```java
package com.autocode.knowledge.parse;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.util.Collections;
import java.util.Map;

public class MarkdownDocumentParser {

    private final String delimiter;
    private final Yaml yaml = new Yaml();

    public MarkdownDocumentParser(String delimiter) {
        if (delimiter == null || delimiter.isBlank()) {
            throw new IllegalArgumentException("frontmatter delimiter must not be blank");
        }
        this.delimiter = delimiter;
    }

    public ParsedMarkdownDocument parse(String content) {
        if (content == null) {
            throw new IllegalArgumentException("content must not be null");
        }
        if (!content.startsWith(delimiter)) {
            return new ParsedMarkdownDocument(false, Collections.emptyMap(), content);
        }

        String[] sections = content.split("\\R" + java.util.regex.Pattern.quote(delimiter) + "\\R", 3);
        if (sections.length < 3 || !sections[0].equals(delimiter)) {
            throw new IllegalArgumentException("frontmatter is not properly closed");
        }

        try {
            Object loaded = yaml.load(sections[1]);
            if (loaded == null) {
                return new ParsedMarkdownDocument(true, Collections.emptyMap(), sections[2]);
            }
            if (!(loaded instanceof Map<?, ?> loadedMap)) {
                throw new IllegalArgumentException("frontmatter must be a YAML object");
            }

            Map<String, Object> frontmatter = new java.util.LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : loadedMap.entrySet()) {
                frontmatter.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            return new ParsedMarkdownDocument(true, frontmatter, sections[2]);
        } catch (YAMLException exception) {
            throw new IllegalArgumentException("Invalid frontmatter", exception);
        }
    }
}
```

- [ ] **Step 7: Register the parser as a Spring bean**

```java
package com.autocode.knowledge.parse;

import com.autocode.knowledge.config.KnowledgeIndexerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MarkdownParserConfiguration {

    @Bean
    MarkdownDocumentParser markdownDocumentParser(KnowledgeIndexerProperties properties) {
        return new MarkdownDocumentParser(properties.frontmatterDelimiter());
    }
}
```

- [ ] **Step 8: Update the module configuration**

```yaml
spring:
  application:
    name: knowledge-indexer
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:local}
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:autocode}
    username: ${DB_USERNAME:autocode}
    password: ${DB_PASSWORD:replace_me}
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:replace_me}
server:
  port: ${KNOWLEDGE_INDEXER_PORT:18085}
management:
  endpoints:
    web:
      exposure:
        include: health,info
logging:
  level:
    root: ${ROOT_LOG_LEVEL:INFO}
    com.autocode: ${APP_LOG_LEVEL:INFO}
autocode:
  knowledge:
    docs-root: ${DOCS_ROOT:./docs}
    markdown-extensions: ${DOC_MARKDOWN_EXTENSIONS:.md,.mdx}
    frontmatter-delimiter: ${DOC_FRONTMATTER_DELIMITER:---}
```

- [ ] **Step 9: Run the module tests**

Run: `mvn -pl knowledge-indexer -am test`

Expected: PASS for the new application, workspace, and parser tests.

### Task 3: Tighten behavior and document completion

**Files:**
- Modify: `backend/knowledge-indexer/src/test/java/com/autocode/knowledge/KnowledgeIndexerApplicationTest.java`
- Modify: `backend/knowledge-indexer/src/test/java/com/autocode/knowledge/workspace/KnowledgeWorkspaceServiceTest.java`
- Modify: `backend/knowledge-indexer/src/test/java/com/autocode/knowledge/parse/MarkdownDocumentParserTest.java`
- Create: `docs/context/progress/2026-05-28-KI-01-Knowledge-Indexer工程初始化进度.md`
- Modify: `docs/context/current/02-当前任务状态.md`
- Modify: `docs/indexes/03-当前任务索引.md`
- Modify: `README.md`

- [ ] **Step 1: Add one more passing test for docs-root creation**

```java
@Test
void shouldCreateDocsRootWhenPreparingWorkspace() {
    Path docsRoot = tempDir.resolve("new-vault");

    KnowledgeWorkspaceService service = new KnowledgeWorkspaceService(
            new KnowledgeIndexerProperties(docsRoot, List.of(".md"), "---")
    );

    Path prepared = service.prepareDocsRoot();

    assertThat(prepared).exists().isDirectory();
    assertThat(prepared).isEqualTo(docsRoot.toAbsolutePath().normalize());
}
```

- [ ] **Step 2: Add one more passing test for empty frontmatter**

```java
@Test
void shouldParseMarkdownWithEmptyFrontmatter() {
    MarkdownDocumentParser parser = new MarkdownDocumentParser("---");

    ParsedMarkdownDocument document = parser.parse("""
            ---
            ---
            # Heading
            """);

    assertThat(document.hasFrontmatter()).isTrue();
    assertThat(document.frontmatter()).isEmpty();
    assertThat(document.body()).contains("# Heading");
}
```

- [ ] **Step 3: Re-run the focused module tests**

Run: `mvn -pl knowledge-indexer -am test`

Expected: PASS with all `knowledge-indexer` tests green.

- [ ] **Step 4: Write the progress note**

```markdown
# 2026-05-28 KI-01 Knowledge Indexer 工程初始化进度

## 已完成

- 补齐 `knowledge-indexer` Spring Boot 启动入口
- 补齐 `autocode.knowledge` 配置属性
- 落地受控文档根目录准备与越界防护服务
- 落地最小 Markdown/frontmatter 解析组件
- 补齐上下文加载、路径校验和解析测试

## 验证

- `mvn -pl knowledge-indexer -am test` 通过

## 结论

`KI-01` 已完成，下一顺位切换到 `CS-01`。
```

- [ ] **Step 5: Update the current task status**

```markdown
- 已完成 `KI-01` Knowledge Indexer 工程初始化
- 已落地 `knowledge-indexer` Spring Boot 启动入口、`autocode.knowledge` 配置、受控文档根目录和最小 Markdown/frontmatter 解析基线
- 已完成 `mvn -pl knowledge-indexer -am test` 验证
```

- [ ] **Step 6: Update the current task index**

```markdown
- 进展：`KI-01` 已完成，下一顺位任务切换为 `CS-01`
```

- [ ] **Step 7: Update the README module status**

```markdown
- `backend/knowledge-indexer`: 已完成 `KI-01` 最小工程初始化，具备 Markdown/frontmatter 识别基线
```

- [ ] **Step 8: Commit**

```bash
git add backend/knowledge-indexer docs/context/progress/2026-05-28-KI-01-Knowledge-Indexer工程初始化进度.md docs/context/current/02-当前任务状态.md docs/indexes/03-当前任务索引.md README.md docs/superpowers/plans/2026-05-28-ki-01-knowledge-indexer.md docs/superpowers/specs/2026-05-28-ki-01-knowledge-indexer-minimal-design.md
git commit -m "feat: initialize knowledge indexer skeleton"
```

## Self-Review

- Spec coverage: startup entrypoint, `autocode.knowledge` config, controlled docs-root service, Markdown/frontmatter parsing, tests, verification, and doc writeback all map to Task 1-3.
- Placeholder scan: no `TODO`, `TBD`, or implicit “handle this later” steps remain.
- Type consistency: `KnowledgeIndexerProperties`, `KnowledgeWorkspaceService`, `MarkdownDocumentParser`, and `ParsedMarkdownDocument` names are consistent across tests and implementation.
