package com.autocode.project.index;

import com.autocode.project.config.ProjectIndexProperties;
import com.autocode.project.domain.ProjectRecord;
import com.autocode.project.service.ProjectIndexSyncException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ProjectIndexSyncService {

    private static final Set<String> SOURCE_EXTENSIONS = Set.of(".java", ".kt", ".ts", ".tsx", ".js", ".jsx", ".vue", ".py", ".go");
    private static final Set<String> DOCUMENT_EXTENSIONS = Set.of(".md", ".markdown");
    private static final Pattern JAVA_TYPE_PATTERN = Pattern.compile("\\b(class|interface|enum|record)\\s+([A-Za-z_][A-Za-z0-9_]*)");
    private static final Pattern JAVA_METHOD_PATTERN = Pattern.compile("\\b(?:public|protected|private)?\\s*(?:static\\s+)?(?:final\\s+)?[A-Za-z0-9_<>,\\[\\]?]+\\s+([A-Za-z_][A-Za-z0-9_]*)\\s*\\(");
    private static final Pattern JS_SYMBOL_PATTERN = Pattern.compile("\\b(function|class|interface|type|const)\\s+([A-Za-z_][A-Za-z0-9_]*)");
    private static final Pattern IMPORT_PATTERN = Pattern.compile("^\\s*import\\s+.*?([A-Za-z_][A-Za-z0-9_]*)\\s*;?\\s*$");
    private static final Pattern MARKDOWN_TITLE_PATTERN = Pattern.compile("^#\\s+(.+)$", Pattern.MULTILINE);
    private static final Pattern REQUIREMENT_CODE_PATTERN = Pattern.compile("\\b[A-Z]{2,3}-\\d{2}\\b");

    private final ProjectIndexRepository projectIndexRepository;
    private final ProjectIndexProperties properties;
    private final ObjectMapper objectMapper;

    public ProjectIndexSyncService(ProjectIndexRepository projectIndexRepository,
                                   ProjectIndexProperties properties,
                                   ObjectMapper objectMapper) {
        this.projectIndexRepository = projectIndexRepository;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public SyncSummary sync(ProjectRecord project, String requestedWorkspaceRoot, Integer requestedMaxCommits) {
        Path workspaceRoot = resolveWorkspaceRoot(requestedWorkspaceRoot);
        if (!Files.isDirectory(workspaceRoot)) {
            throw new ProjectIndexSyncException("Workspace root does not exist: " + workspaceRoot);
        }

        // Each sync rebuilds a fresh snapshot so the query layer always reads one coherent dataset.
        projectIndexRepository.deleteProjectIndexData(project.id());

        List<SourceFile> sourceFiles = collectFiles(workspaceRoot, SOURCE_EXTENSIONS, properties.maxSourceFiles());
        List<DocumentFile> documentFiles = collectDocuments(workspaceRoot, project.docRepoPath(), properties.maxDocuments());
        List<SymbolDraft> symbols = extractSymbols(sourceFiles, workspaceRoot);

        Map<String, List<StoredSymbol>> symbolsByFile = new LinkedHashMap<>();
        Map<String, List<StoredSymbol>> symbolsByName = new LinkedHashMap<>();
        int symbolCount = 0;
        for (SymbolDraft symbol : symbols) {
            long symbolId = projectIndexRepository.insertSymbol(
                    project.id(),
                    symbol.filePath(),
                    symbol.name(),
                    symbol.kind(),
                    symbol.signature(),
                    symbol.lineStart(),
                    symbol.lineEnd()
            );
            StoredSymbol storedSymbol = new StoredSymbol(symbolId, symbol.filePath(), symbol.name(), symbol.kind());
            symbolsByFile.computeIfAbsent(symbol.filePath(), ignored -> new ArrayList<>()).add(storedSymbol);
            symbolsByName.computeIfAbsent(symbol.name(), ignored -> new ArrayList<>()).add(storedSymbol);
            symbolCount++;
        }

        int edgeCount = insertEdges(project.id(), sourceFiles, workspaceRoot, symbolsByFile, symbolsByName);

        GitSnapshot gitSnapshot = loadGitSnapshot(workspaceRoot, requestedMaxCommits);
        int commitCount = 0;
        int commitSymbolCount = 0;
        Map<String, List<StoredSymbol>> symbolsByNormalizedFile = symbolsByFile.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> normalizePath(entry.getKey()),
                        Map.Entry::getValue,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        for (GitCommitDraft commit : gitSnapshot.commits()) {
            long commitId = projectIndexRepository.insertCommit(
                    project.id(),
                    commit.hash(),
                    commit.author(),
                    commit.commitTime(),
                    commit.message(),
                    gitSnapshot.branchName()
            );
            commitCount++;
            Set<Long> linkedSymbolIds = new LinkedHashSet<>();
            for (String changedFile : commit.changedFiles()) {
                List<StoredSymbol> linkedSymbols = symbolsByNormalizedFile.getOrDefault(normalizePath(changedFile), List.of());
                for (StoredSymbol symbol : linkedSymbols) {
                    if (linkedSymbolIds.add(symbol.id())) {
                        projectIndexRepository.insertCommitSymbol(project.id(), commitId, symbol.id(), "MODIFIED");
                        commitSymbolCount++;
                    }
                }
            }
        }

        int documentCount = 0;
        int requirementCount = 0;
        int linkCount = 0;
        Map<String, Long> requirementsByCode = new LinkedHashMap<>();
        Map<String, Set<Long>> linkedTargetsByDocument = new HashMap<>();
        for (DocumentFile document : documentFiles) {
            long documentId = projectIndexRepository.insertDocument(
                    project.id(),
                    document.relativePath(),
                    "markdown",
                    document.title(),
                    metadataJson(document)
            );
            documentCount++;

            Set<String> requirementCodes = extractRequirementCodes(document.content());
            for (String requirementCode : requirementCodes) {
                long requirementId = projectIndexRepository.insertRequirement(
                        project.id(),
                        requirementCode,
                        document.title(),
                        documentId
                );
                requirementsByCode.put(requirementCode, requirementId);
                requirementCount++;
                linkCount += linkDocumentToRequirement(project.id(), documentId, requirementId, linkedTargetsByDocument);
            }

            Set<Long> symbolLinks = matchDocumentSymbols(document.content(), symbolsByName);
            for (Long symbolId : symbolLinks) {
                linkCount += linkDocumentToSymbol(project.id(), documentId, symbolId, linkedTargetsByDocument);
            }
        }

        return new SyncSummary(
                workspaceRoot.toAbsolutePath().normalize().toString(),
                symbolCount,
                edgeCount,
                commitCount,
                documentCount,
                requirementCount,
                linkCount
        );
    }

    private int insertEdges(long projectId,
                            List<SourceFile> sourceFiles,
                            Path workspaceRoot,
                            Map<String, List<StoredSymbol>> symbolsByFile,
                            Map<String, List<StoredSymbol>> symbolsByName) {
        int edgeCount = 0;
        Set<String> createdEdges = new HashSet<>();
        for (SourceFile sourceFile : sourceFiles) {
            String relativePath = relativePath(workspaceRoot, sourceFile.path());
            List<StoredSymbol> sourceSymbols = symbolsByFile.getOrDefault(relativePath, List.of());
            if (sourceSymbols.isEmpty()) {
                continue;
            }
            Set<String> importedNames = extractImportedNames(sourceFile.content());
            for (String importedName : importedNames) {
                for (StoredSymbol sourceSymbol : sourceSymbols) {
                    for (StoredSymbol targetSymbol : symbolsByName.getOrDefault(importedName, List.of())) {
                        if (sourceSymbol.id() == targetSymbol.id()) {
                            continue;
                        }
                        String edgeKey = sourceSymbol.id() + "->" + targetSymbol.id() + ":IMPORTS";
                        if (createdEdges.add(edgeKey)) {
                            projectIndexRepository.insertEdge(projectId, sourceSymbol.id(), targetSymbol.id(), "IMPORTS");
                            edgeCount++;
                        }
                    }
                }
            }
        }
        return edgeCount;
    }

    private GitSnapshot loadGitSnapshot(Path workspaceRoot, Integer requestedMaxCommits) {
        int maxCommits = requestedMaxCommits == null || requestedMaxCommits <= 0
                ? properties.maxCommits()
                : Math.min(requestedMaxCommits, properties.maxCommits());
        Path gitDir = workspaceRoot.resolve(".git");
        if (!Files.exists(gitDir)) {
            return new GitSnapshot("unknown", List.of());
        }

        try {
            String branchName = firstLine(runCommand(workspaceRoot, List.of("git", "rev-parse", "--abbrev-ref", "HEAD")));
            String logOutput = runCommand(
                    workspaceRoot,
                    List.of("git", "log", "--date=iso-strict", "--pretty=format:%H\t%an\t%aI\t%s", "-n", String.valueOf(maxCommits))
            );
            List<GitCommitDraft> commits = new ArrayList<>();
            for (String line : splitLines(logOutput)) {
                String[] parts = line.split("\t", 4);
                if (parts.length < 4) {
                    continue;
                }
                List<String> changedFiles = splitLines(runCommand(
                        workspaceRoot,
                        List.of("git", "diff-tree", "--no-commit-id", "--name-only", "-r", parts[0])
                ));
                commits.add(new GitCommitDraft(parts[0], parts[1], parts[2], parts[3], changedFiles));
            }
            return new GitSnapshot(branchName == null || branchName.isBlank() ? "unknown" : branchName, commits);
        } catch (ProjectIndexSyncException exception) {
            return new GitSnapshot("unknown", List.of());
        }
    }

    private List<SymbolDraft> extractSymbols(List<SourceFile> sourceFiles, Path workspaceRoot) {
        List<SymbolDraft> symbols = new ArrayList<>();
        for (SourceFile sourceFile : sourceFiles) {
            String relativePath = relativePath(workspaceRoot, sourceFile.path());
            String[] lines = sourceFile.content().split("\\R", -1);
            for (int index = 0; index < lines.length; index++) {
                String line = lines[index];
                // The parser stays intentionally shallow: we only need stable lookup anchors for queries.
                addMatchedSymbol(symbols, relativePath, index, line, JAVA_TYPE_PATTERN, "type");
                addMatchedSymbol(symbols, relativePath, index, line, JAVA_METHOD_PATTERN, "method");
                addMatchedSymbol(symbols, relativePath, index, line, JS_SYMBOL_PATTERN, "symbol");
            }
        }
        return symbols;
    }

    private void addMatchedSymbol(List<SymbolDraft> symbols,
                                  String relativePath,
                                  int index,
                                  String line,
                                  Pattern pattern,
                                  String fallbackKind) {
        Matcher matcher = pattern.matcher(line);
        if (!matcher.find()) {
            return;
        }
        String kind = matcher.groupCount() >= 2 ? matcher.group(1) : fallbackKind;
        String name = matcher.group(matcher.groupCount() >= 2 ? 2 : 1);
        if (name == null || name.isBlank()) {
            return;
        }
        symbols.add(new SymbolDraft(
                relativePath,
                name.trim(),
                normalizeKind(kind, fallbackKind),
                line.trim(),
                index + 1,
                index + 1
        ));
    }

    private String normalizeKind(String rawKind, String fallbackKind) {
        if (rawKind == null || rawKind.isBlank()) {
            return fallbackKind;
        }
        String normalized = rawKind.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "class", "interface", "enum", "record", "function", "type", "const", "method" -> normalized;
            default -> fallbackKind;
        };
    }

    private List<SourceFile> collectFiles(Path workspaceRoot, Set<String> extensions, int maxFiles) {
        List<SourceFile> files = new ArrayList<>();
        walkFiles(workspaceRoot, path -> {
            if (files.size() >= maxFiles) {
                return;
            }
            if (extensions.contains(extension(path))) {
                files.add(new SourceFile(path, safeRead(path)));
            }
        });
        return files;
    }

    private List<DocumentFile> collectDocuments(Path workspaceRoot, String docRepoPath, int maxDocuments) {
        Path documentsRoot = docRepoPath == null || docRepoPath.isBlank()
                ? workspaceRoot.resolve("docs")
                : workspaceRoot.resolve(docRepoPath);
        if (!Files.exists(documentsRoot)) {
            return List.of();
        }

        List<DocumentFile> documents = new ArrayList<>();
        walkFiles(documentsRoot, path -> {
            if (documents.size() >= maxDocuments) {
                return;
            }
            if (DOCUMENT_EXTENSIONS.contains(extension(path))) {
                String content = safeRead(path);
                documents.add(new DocumentFile(
                        relativePath(workspaceRoot, path),
                        titleForDocument(path, content),
                        excerptForDocument(content),
                        content
                ));
            }
        });
        return documents;
    }

    private void walkFiles(Path root, ThrowingConsumer<Path> consumer) {
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    String name = dir.getFileName() == null ? "" : dir.getFileName().toString();
                    for (String excluded : properties.excludedDirectories()) {
                        if (name.equalsIgnoreCase(excluded)) {
                            return FileVisitResult.SKIP_SUBTREE;
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    consumer.accept(file);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException exception) {
            throw new ProjectIndexSyncException("Failed to scan workspace files", exception);
        }
    }

    private String metadataJson(DocumentFile document) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("excerpt", document.excerpt());
        metadata.put("indexedAt", System.currentTimeMillis());
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException exception) {
            throw new ProjectIndexSyncException("Failed to serialize document metadata", exception);
        }
    }

    private Set<String> extractRequirementCodes(String content) {
        Set<String> codes = new LinkedHashSet<>();
        Matcher matcher = REQUIREMENT_CODE_PATTERN.matcher(content);
        while (matcher.find()) {
            codes.add(matcher.group());
        }
        return codes;
    }

    private Set<Long> matchDocumentSymbols(String content, Map<String, List<StoredSymbol>> symbolsByName) {
        String normalizedContent = content.toLowerCase(Locale.ROOT);
        Set<Long> symbolIds = new LinkedHashSet<>();
        for (Map.Entry<String, List<StoredSymbol>> entry : symbolsByName.entrySet()) {
            String symbolName = entry.getKey();
            if (symbolName.length() < 4) {
                continue;
            }
            if (normalizedContent.contains(symbolName.toLowerCase(Locale.ROOT))) {
                entry.getValue().stream()
                        .map(StoredSymbol::id)
                        .forEach(symbolIds::add);
            }
        }
        return symbolIds;
    }

    private int linkDocumentToRequirement(long projectId,
                                          long documentId,
                                          long requirementId,
                                          Map<String, Set<Long>> linkedTargetsByDocument) {
        String key = documentId + ":requirement";
        if (!linkedTargetsByDocument.computeIfAbsent(key, ignored -> new LinkedHashSet<>()).add(requirementId)) {
            return 0;
        }
        projectIndexRepository.insertDocumentLink(projectId, documentId, null, null, requirementId);
        return 1;
    }

    private int linkDocumentToSymbol(long projectId,
                                     long documentId,
                                     long symbolId,
                                     Map<String, Set<Long>> linkedTargetsByDocument) {
        String key = documentId + ":symbol";
        if (!linkedTargetsByDocument.computeIfAbsent(key, ignored -> new LinkedHashSet<>()).add(symbolId)) {
            return 0;
        }
        projectIndexRepository.insertDocumentLink(projectId, documentId, symbolId, null, null);
        return 1;
    }

    private Set<String> extractImportedNames(String content) {
        Set<String> importedNames = new LinkedHashSet<>();
        for (String line : content.split("\\R")) {
            Matcher matcher = IMPORT_PATTERN.matcher(line);
            if (matcher.find()) {
                importedNames.add(matcher.group(1));
            }
        }
        return importedNames;
    }

    private String titleForDocument(Path path, String content) {
        Matcher matcher = MARKDOWN_TITLE_PATTERN.matcher(content);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return path.getFileName().toString();
    }

    private String excerptForDocument(String content) {
        for (String line : content.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.equals("---")) {
                continue;
            }
            return trimmed.length() > 280 ? trimmed.substring(0, 280) : trimmed;
        }
        return "";
    }

    private Path resolveWorkspaceRoot(String requestedWorkspaceRoot) {
        if (requestedWorkspaceRoot != null && !requestedWorkspaceRoot.isBlank()) {
            return Path.of(requestedWorkspaceRoot).toAbsolutePath().normalize();
        }

        if (properties.defaultWorkspaceRoot() != null && !properties.defaultWorkspaceRoot().isBlank()) {
            return Path.of(properties.defaultWorkspaceRoot()).toAbsolutePath().normalize();
        }

        Path current = Path.of("").toAbsolutePath().normalize();
        while (current != null) {
            if (Files.exists(current.resolve(".git"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new ProjectIndexSyncException("Unable to auto-detect workspace root");
    }

    private String relativePath(Path root, Path path) {
        return normalizePath(root.relativize(path.toAbsolutePath().normalize()).toString());
    }

    private String normalizePath(String value) {
        return value.replace('\\', '/');
    }

    private String safeRead(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new ProjectIndexSyncException("Failed to read file: " + path, exception);
        }
    }

    private String extension(Path path) {
        String fileName = path.getFileName() == null ? "" : path.getFileName().toString();
        int index = fileName.lastIndexOf('.');
        if (index < 0) {
            return "";
        }
        return fileName.substring(index).toLowerCase(Locale.ROOT);
    }

    private String runCommand(Path workingDirectory, List<String> command) {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(workingDirectory.toFile());
        builder.redirectErrorStream(true);
        try {
            Process process = builder.start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new ProjectIndexSyncException("Command failed: " + String.join(" ", command) + System.lineSeparator() + output);
            }
            return output;
        } catch (IOException | InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ProjectIndexSyncException("Failed to run command: " + String.join(" ", command), exception);
        }
    }

    private List<String> splitLines(String output) {
        return output.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .toList();
    }

    private String firstLine(String value) {
        return value == null ? null : value.lines().findFirst().orElse(null);
    }

    @FunctionalInterface
    private interface ThrowingConsumer<T> {
        void accept(T value) throws IOException;
    }

    private record SourceFile(Path path, String content) {
    }

    private record DocumentFile(String relativePath, String title, String excerpt, String content) {
    }

    private record SymbolDraft(String filePath, String name, String kind, String signature, int lineStart, int lineEnd) {
    }

    private record StoredSymbol(long id, String filePath, String name, String kind) {
    }

    private record GitCommitDraft(String hash, String author, String commitTime, String message, List<String> changedFiles) {
    }

    private record GitSnapshot(String branchName, List<GitCommitDraft> commits) {
    }

    public record SyncSummary(
            String workspaceRoot,
            int symbolCount,
            int edgeCount,
            int commitCount,
            int documentCount,
            int requirementCount,
            int linkCount
    ) {
    }
}
