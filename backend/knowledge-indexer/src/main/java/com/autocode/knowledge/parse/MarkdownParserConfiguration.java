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
