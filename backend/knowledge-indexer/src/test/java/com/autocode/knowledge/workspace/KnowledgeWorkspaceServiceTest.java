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
    void shouldCreateDocsRootWhenPreparingWorkspace() {
        Path docsRoot = tempDir.resolve("new-vault");

        KnowledgeWorkspaceService service = new KnowledgeWorkspaceService(
                new KnowledgeIndexerProperties(docsRoot, List.of(".md"), "---")
        );

        Path prepared = service.prepareDocsRoot();

        assertThat(prepared).exists().isDirectory();
        assertThat(prepared).isEqualTo(docsRoot.toAbsolutePath().normalize());
    }

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
