package com.autocode.codegraph.index;

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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CodegraphIndexService {

    private static final Logger log = LoggerFactory.getLogger(CodegraphIndexService.class);

    private static final Set<String> SOURCE_EXTENSIONS = Set.of(
            ".java", ".kt", ".ts", ".tsx", ".js", ".jsx", ".vue", ".py", ".go");

    private static final Pattern JAVA_TYPE_PATTERN =
            Pattern.compile("\\b(class|interface|enum|record)\\s+([A-Za-z_][A-Za-z0-9_]*)");
    private static final Pattern JAVA_METHOD_PATTERN =
            Pattern.compile("\\b(?:public|protected|private)?\\s*(?:static\\s+)?(?:final\\s+)?[A-Za-z0-9_<>,\\[\\]?]+\\s+([A-Za-z_][A-Za-z0-9_]*)\\s*\\(");
    private static final Pattern JS_SYMBOL_PATTERN =
            Pattern.compile("\\b(function|class|interface|type|const)\\s+([A-Za-z_][A-Za-z0-9_]*)");
    private static final Pattern IMPORT_PATTERN =
            Pattern.compile("^\\s*import\\s+.*?([A-Za-z_][A-Za-z0-9_]*)\\s*;?\\s*$");

    private final CodegraphIndexRepository repository;

    public CodegraphIndexService(CodegraphIndexRepository repository) {
        this.repository = repository;
    }

    public CodegraphIndexResult index(long projectId, Path workspaceRoot) {
        if (!Files.isDirectory(workspaceRoot)) {
            throw new IllegalArgumentException("Workspace root does not exist: " + workspaceRoot);
        }

        repository.deleteProjectData(projectId);

        List<SourceFile> sourceFiles = collectFiles(workspaceRoot);
        log.info("Collected {} source files for project {}", sourceFiles.size(), projectId);

        List<SymbolDraft> symbols = extractSymbols(sourceFiles, workspaceRoot);
        log.info("Extracted {} symbols for project {}", symbols.size(), projectId);

        Map<String, List<StoredSymbol>> symbolsByFile = new LinkedHashMap<>();
        Map<String, List<StoredSymbol>> symbolsByName = new LinkedHashMap<>();
        int symbolCount = 0;

        for (SymbolDraft symbol : symbols) {
            long symbolId = repository.insertSymbol(
                    projectId, symbol.filePath, symbol.name, symbol.kind,
                    symbol.signature, symbol.lineStart, symbol.lineEnd);
            StoredSymbol stored = new StoredSymbol(symbolId, symbol.filePath, symbol.name, symbol.kind);
            symbolsByFile.computeIfAbsent(symbol.filePath, k -> new ArrayList<>()).add(stored);
            symbolsByName.computeIfAbsent(symbol.name, k -> new ArrayList<>()).add(stored);
            symbolCount++;
        }

        int edgeCount = insertEdges(projectId, sourceFiles, workspaceRoot, symbolsByFile, symbolsByName);
        log.info("Inserted {} edges for project {}", edgeCount, projectId);

        return new CodegraphIndexResult(symbolCount, edgeCount);
    }

    private List<SourceFile> collectFiles(Path root) {
        List<SourceFile> files = new ArrayList<>();
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    String ext = extension(file);
                    if (SOURCE_EXTENSIONS.contains(ext)) {
                        files.add(new SourceFile(file, safeRead(file)));
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new RuntimeException("Failed to walk workspace: " + root, e);
        }
        return files;
    }

    private List<SymbolDraft> extractSymbols(List<SourceFile> files, Path root) {
        List<SymbolDraft> symbols = new ArrayList<>();
        for (SourceFile sf : files) {
            String relPath = relativePath(root, sf.path);
            String ext = extension(sf.path);

            if (ext.equals(".java") || ext.equals(".kt")) {
                extractJavaSymbols(sf.content, relPath, symbols);
            } else if (ext.equals(".ts") || ext.equals(".tsx") || ext.equals(".js") || ext.equals(".jsx")) {
                extractJsSymbols(sf.content, relPath, symbols);
            } else if (ext.equals(".py")) {
                extractPySymbols(sf.content, relPath, symbols);
            }
        }
        return symbols;
    }

    private void extractJavaSymbols(String content, String relPath, List<SymbolDraft> out) {
        int lineNum = 1;
        for (String line : content.split("\\R")) {
            Matcher m = JAVA_TYPE_PATTERN.matcher(line);
            if (m.find()) {
                out.add(new SymbolDraft(relPath, m.group(2), m.group(1), line.trim(), lineNum, lineNum));
            }
            m = JAVA_METHOD_PATTERN.matcher(line);
            if (m.find()) {
                out.add(new SymbolDraft(relPath, m.group(1), "method", line.trim(), lineNum, lineNum));
            }
            lineNum++;
        }
    }

    private void extractJsSymbols(String content, String relPath, List<SymbolDraft> out) {
        int lineNum = 1;
        for (String line : content.split("\\R")) {
            Matcher m = JS_SYMBOL_PATTERN.matcher(line);
            if (m.find()) {
                out.add(new SymbolDraft(relPath, m.group(2), m.group(1), line.trim(), lineNum, lineNum));
            }
            lineNum++;
        }
    }

    private void extractPySymbols(String content, String relPath, List<SymbolDraft> out) {
        int lineNum = 1;
        for (String line : content.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("def ")) {
                String name = trimmed.substring(4).split("\\(")[0].trim();
                out.add(new SymbolDraft(relPath, name, "function", trimmed, lineNum, lineNum));
            } else if (trimmed.startsWith("class ")) {
                String name = trimmed.substring(6).split("[:(]")[0].trim();
                out.add(new SymbolDraft(relPath, name, "class", trimmed, lineNum, lineNum));
            }
            lineNum++;
        }
    }

    private int insertEdges(long projectId, List<SourceFile> files, Path root,
                            Map<String, List<StoredSymbol>> symbolsByFile,
                            Map<String, List<StoredSymbol>> symbolsByName) {
        int edgeCount = 0;
        for (SourceFile sf : files) {
            String relPath = relativePath(root, sf.path);
            Set<String> importedNames = extractImports(sf.content);
            List<StoredSymbol> fileSymbols = symbolsByFile.getOrDefault(relPath, List.of());

            for (String imported : importedNames) {
                List<StoredSymbol> targets = symbolsByName.getOrDefault(imported, List.of());
                for (StoredSymbol source : fileSymbols) {
                    for (StoredSymbol target : targets) {
                        if (source.id != target.id) {
                            repository.insertEdge(projectId, source.id, target.id, "IMPORTS");
                            edgeCount++;
                        }
                    }
                }
            }

            for (int i = 0; i < fileSymbols.size(); i++) {
                for (int j = i + 1; j < fileSymbols.size(); j++) {
                    StoredSymbol a = fileSymbols.get(i);
                    StoredSymbol b = fileSymbols.get(j);
                    if (a.kind.equals("method") && b.kind.equals("method")) {
                        String content = sf.content.toLowerCase(Locale.ROOT);
                        if (content.contains(b.name.toLowerCase(Locale.ROOT))) {
                            repository.insertEdge(projectId, a.id, b.id, "CALLS");
                            edgeCount++;
                        }
                    }
                }
            }
        }
        return edgeCount;
    }

    private Set<String> extractImports(String content) {
        Set<String> names = new LinkedHashSet<>();
        for (String line : content.split("\\R")) {
            Matcher m = IMPORT_PATTERN.matcher(line);
            if (m.find()) {
                names.add(m.group(1));
            }
        }
        return names;
    }

    private String relativePath(Path root, Path path) {
        return root.relativize(path.toAbsolutePath().normalize()).toString().replace('\\', '/');
    }

    private String extension(Path path) {
        String name = path.getFileName() == null ? "" : path.getFileName().toString();
        int idx = name.lastIndexOf('.');
        return idx < 0 ? "" : name.substring(idx).toLowerCase(Locale.ROOT);
    }

    private String safeRead(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file: " + path, e);
        }
    }

    private record SourceFile(Path path, String content) {}
    private record SymbolDraft(String filePath, String name, String kind, String signature, int lineStart, int lineEnd) {}
    private record StoredSymbol(long id, String filePath, String name, String kind) {}

    public record CodegraphIndexResult(int symbolCount, int edgeCount) {}
}