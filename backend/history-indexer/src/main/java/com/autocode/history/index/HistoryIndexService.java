package com.autocode.history.index;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class HistoryIndexService {

    private static final Logger log = LoggerFactory.getLogger(HistoryIndexService.class);

    private static final Pattern REQUIREMENT_PATTERN =
            Pattern.compile("\\b[A-Z]{2,3}-\\d{2,3}\\b");

    private final HistoryIndexRepository repository;

    public HistoryIndexService(HistoryIndexRepository repository) {
        this.repository = repository;
    }

    public HistoryIndexResult index(long projectId, Path workspaceRoot, Integer maxCommits) {
        int limit = maxCommits != null ? maxCommits : 80;

        repository.deleteProjectData(projectId);

        List<GitCommit> commits = loadGitHistory(workspaceRoot, limit);
        log.info("Loaded {} commits for project {}", commits.size(), projectId);

        int commitCount = 0;
        int linkCount = 0;

        for (GitCommit commit : commits) {
            long commitId = repository.insertCommit(
                    projectId, commit.hash, commit.author,
                    commit.commitTime, commit.message, commit.branch);

            List<String> requirementCodes = extractRequirements(commit.message);
            for (String code : requirementCodes) {
                linkCount++;
            }

            commitCount++;
        }

        log.info("History index complete: {} commits, {} links", commitCount, linkCount);
        return new HistoryIndexResult(commitCount, linkCount);
    }

    private List<GitCommit> loadGitHistory(Path workspaceRoot, int maxCommits) {
        List<GitCommit> commits = new ArrayList<>();
        try {
            String output = runCommand(workspaceRoot,
                    "git", "log",
                    "--format=%H%x00%an%x00%aI%x00%s%x00%D",
                    "-n", String.valueOf(maxCommits));

            for (String line : output.split("\\R")) {
                if (line.isBlank()) continue;
                String[] parts = line.split("\0");
                if (parts.length >= 4) {
                    String branch = "main";
                    if (parts.length >= 5 && !parts[4].isBlank()) {
                        String ref = parts[4].trim();
                        if (ref.startsWith("HEAD -> ")) {
                            branch = ref.substring(8).split(",")[0].trim();
                        } else {
                            branch = ref.split(",")[0].trim();
                        }
                    }
                    commits.add(new GitCommit(parts[0].trim(), parts[1].trim(),
                            parts[2].trim(), parts[3].trim(), branch));
                }
            }
        } catch (Exception e) {
            log.warn("Failed to load git history from {}: {}", workspaceRoot, e.getMessage());
        }
        return commits;
    }

    private List<String> extractRequirements(String message) {
        List<String> codes = new ArrayList<>();
        Matcher m = REQUIREMENT_PATTERN.matcher(message);
        while (m.find()) {
            codes.add(m.group());
        }
        return codes;
    }

    private String runCommand(Path workingDirectory, String... command) {
        try {
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.directory(workingDirectory.toFile());
            builder.redirectErrorStream(true);
            Process process = builder.start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("Command failed: " + String.join(" ", command));
            }
            return output;
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to run git command", e);
        }
    }

    private record GitCommit(String hash, String author, String commitTime, String message, String branch) {}

    public record HistoryIndexResult(int commitCount, int linkCount) {}
}