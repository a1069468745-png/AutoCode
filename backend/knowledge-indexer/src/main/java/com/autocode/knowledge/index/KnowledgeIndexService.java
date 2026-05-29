package com.autocode.knowledge.index;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class KnowledgeIndexService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeIndexService.class);

    private static final Set<String> DOCUMENT_EXTENSIONS = Set.of(".md", ".markdown");
    private static final Pattern TITLE_PATTERN = Pattern.compile("^#\\s+(.+)$", Pattern.MULTILINE);
    private static final Pattern REQUIREMENT_CODE_PATTERN = Pattern.compile("\\b[A-Z]{2,3}-\\d{2,3}\\b");
    private static final Pattern FRONTMATTER_PATTERN = Pattern.compile("^---\\s*\\n(.*?)\\n---", Pattern.DOTALL);

    private final KnowledgeIndexRepository repository;
    private final ObjectMapper objectMapper;

    public KnowledgeIndexService(KnowledgeIndexRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public KnowledgeIndexResult index(long projectId, Path workspaceRoot, String docRepoPath) {
        Path scanRoot = docRepoPath != null && !docRepoPath.isBlank()
                ? workspaceRoot.resolve(docRepoPath).normalize()
                : workspaceRoot;

        if (!Files.isDirectory(scanRoot)) {
            log.warn("Document scan root does not exist: {}", scanRoot);
            return new KnowledgeIndexResult(0, 0, 0);
        }

        repository.deleteProjectData(projectId);

        List<DocumentDraft> documents = collectDocuments(scanRoot, workspaceRoot);
        log.info("Collected {} documents for project {}", documents.size(), projectId);

        int docCount = 0;
        int reqCount = 0;
        int linkCount = 0;

        for (DocumentDraft doc : documents) {
            long docId = repository.insertDocument(
                    projectId, doc.relativePath, "spec", doc.title, doc.metadataJson);
            docCount++;

            List<String> codes = extractRequirementCodes(doc.content);
            for (String code : codes) {
                long reqId = repository.insertRequirement(projectId, code, doc.title, docId);
                reqCount++;
                repository.insertDocumentLink(projectId, docId, null, null, reqId);
                linkCount++;
            }
        }

        log.info("Knowledge index complete: {} docs, {} requirements, {} links", docCount, reqCount, linkCount);
        return new KnowledgeIndexResult(docCount, reqCount, linkCount);
    }

    private List<DocumentDraft> collectDocuments(Path scanRoot, Path workspaceRoot) {
        List<DocumentDraft> docs = new ArrayList<>();
        try {
            Files.walkFileTree(scanRoot, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    String ext = extension(file);
                    if (DOCUMENT_EXTENSIONS.contains(ext)) {
                        String content = safeRead(file);
                        String title = extractTitle(file, content);
                        String metadataJson = extractMetadataJson(content);
                        String relPath = workspaceRoot.relativize(file.toAbsolutePath().normalize())
                                .toString().replace('\\', '/');
                        docs.add(new DocumentDraft(relPath, title, metadataJson, content));
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new RuntimeException("Failed to scan documents: " + scanRoot, e);
        }
        return docs;
    }

    private String extractTitle(Path path, String content) {
        Matcher m = TITLE_PATTERN.matcher(content);
        if (m.find()) {
            return m.group(1).trim();
        }
        return path.getFileName().toString();
    }

    private String extractMetadataJson(String content) {
        Matcher m = FRONTMATTER_PATTERN.matcher(content);
        if (m.find()) {
            try {
                // Simple YAML-like key-value extraction
                Map<String, String> meta = new LinkedHashMap<>();
                for (String line : m.group(1).split("\\R")) {
                    int colonIdx = line.indexOf(':');
                    if (colonIdx > 0) {
                        String key = line.substring(0, colonIdx).trim();
                        String value = line.substring(colonIdx + 1).trim();
                        meta.put(key, value);
                    }
                }
                return objectMapper.writeValueAsString(meta);
            } catch (JsonProcessingException e) {
                return "{}";
            }
        }
        return "{}";
    }

    private List<String> extractRequirementCodes(String content) {
        List<String> codes = new ArrayList<>();
        Matcher m = REQUIREMENT_CODE_PATTERN.matcher(content);
        while (m.find()) {
            codes.add(m.group());
        }
        return codes;
    }

    private String extension(Path path) {
        String name = path.getFileName() == null ? "" : path.getFileName().toString();
        int idx = name.lastIndexOf('.');
        return idx < 0 ? "" : name.substring(idx).toLowerCase();
    }

    private String safeRead(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file: " + path, e);
        }
    }

    private record DocumentDraft(String relativePath, String title, String metadataJson, String content) {}

    public record KnowledgeIndexResult(int documentCount, int requirementCount, int linkCount) {}
}