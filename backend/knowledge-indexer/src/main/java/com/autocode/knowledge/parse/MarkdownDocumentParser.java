package com.autocode.knowledge.parse;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class MarkdownDocumentParser {

    private final String delimiter;
    private final Yaml yaml;

    public MarkdownDocumentParser(String delimiter) {
        if (delimiter == null || delimiter.isBlank()) {
            throw new IllegalArgumentException("frontmatter delimiter must not be blank");
        }
        this.delimiter = delimiter;
        this.yaml = new Yaml();
    }

    public ParsedMarkdownDocument parse(String content) {
        if (content == null) {
            throw new IllegalArgumentException("content must not be null");
        }

        String normalizedContent = content.replace("\r\n", "\n");
        String openingMarker = delimiter + "\n";
        if (!normalizedContent.startsWith(openingMarker)) {
            return new ParsedMarkdownDocument(false, Collections.emptyMap(), content);
        }

        int frontmatterStart = openingMarker.length();
        String closingMarker = "\n" + delimiter;
        int closingIndex = normalizedContent.indexOf(closingMarker, frontmatterStart);
        if (closingIndex < 0) {
            throw new IllegalArgumentException("frontmatter is not properly closed");
        }

        String frontmatterContent = normalizedContent.substring(frontmatterStart, closingIndex);
        int bodyStart = closingIndex + closingMarker.length();
        if (bodyStart < normalizedContent.length() && normalizedContent.charAt(bodyStart) == '\n') {
            bodyStart++;
        }
        String body = normalizedContent.substring(bodyStart);

        try {
            Object loaded = yaml.load(frontmatterContent);
            if (loaded == null) {
                return new ParsedMarkdownDocument(true, Collections.emptyMap(), body);
            }
            if (!(loaded instanceof Map<?, ?> loadedMap)) {
                throw new IllegalArgumentException("frontmatter must be a YAML object");
            }

            Map<String, Object> frontmatter = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : loadedMap.entrySet()) {
                frontmatter.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            return new ParsedMarkdownDocument(true, frontmatter, body);
        } catch (YAMLException exception) {
            throw new IllegalArgumentException("Invalid frontmatter", exception);
        }
    }
}
