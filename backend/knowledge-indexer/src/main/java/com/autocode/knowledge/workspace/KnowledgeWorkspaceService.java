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
        if (properties == null || properties.docsRoot() == null) {
            throw new IllegalArgumentException("docsRoot must not be null");
        }
        this.docsRoot = properties.docsRoot().toAbsolutePath().normalize();
    }

    public Path docsRoot() {
        return docsRoot;
    }

    public Path prepareDocsRoot() {
        try {
            return Files.createDirectories(docsRoot).toAbsolutePath().normalize();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to prepare docs root: " + docsRoot, exception);
        }
    }

    public Path resolveDocument(String documentPath) {
        if (documentPath == null || documentPath.isBlank()) {
            throw new IllegalArgumentException("documentPath must not be blank");
        }

        Path resolved = docsRoot.resolve(documentPath).normalize().toAbsolutePath();
        if (!resolved.startsWith(docsRoot)) {
            throw new IllegalArgumentException("documentPath must stay inside docs root");
        }
        return resolved;
    }
}
