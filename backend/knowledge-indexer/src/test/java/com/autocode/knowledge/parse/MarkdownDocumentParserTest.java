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
        assertThat(document.body()).isEqualTo("""
                # Heading

                body
                """);
    }

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
        assertThat(document.body()).isEqualTo("# Heading\n");
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
        assertThat(document.body()).isEqualTo("""
                # Heading

                plain body
                """);
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
