package com.autocode.knowledge.parse;

import java.util.Map;

public record ParsedMarkdownDocument(
        boolean hasFrontmatter,
        Map<String, Object> frontmatter,
        String body
) {
}
