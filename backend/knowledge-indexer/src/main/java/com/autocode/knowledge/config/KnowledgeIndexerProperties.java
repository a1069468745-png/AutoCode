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
