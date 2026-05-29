package com.autocode.knowledge;

import com.autocode.knowledge.config.KnowledgeIndexerProperties;
import com.autocode.knowledge.workspace.KnowledgeWorkspaceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Path;

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
