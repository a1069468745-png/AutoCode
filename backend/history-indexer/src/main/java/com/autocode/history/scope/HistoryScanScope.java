package com.autocode.history.scope;

import java.util.Optional;

public record HistoryScanScope(
        String branchName,
        Optional<String> fromCommit,
        Optional<String> toCommit,
        int maxCommitWindow
) {

    public static HistoryScanScope of(String branchName, String fromCommit, String toCommit, int maxCommitWindow) {
        if (branchName == null || branchName.isBlank() || branchName.contains("..")) {
            throw new IllegalArgumentException("branchName must not be blank or contain traversal segments");
        }
        if (maxCommitWindow <= 0) {
            throw new IllegalArgumentException("maxCommitWindow must be greater than zero");
        }
        boolean onlyOneCommitProvided = (fromCommit == null) ^ (toCommit == null);
        if (onlyOneCommitProvided) {
            throw new IllegalArgumentException("commit range must provide both fromCommit and toCommit");
        }

        return new HistoryScanScope(
                branchName.trim(),
                Optional.ofNullable(fromCommit),
                Optional.ofNullable(toCommit),
                maxCommitWindow
        );
    }
}
