package com.autocode.project.service;

public class ProjectIndexSyncException extends RuntimeException {
    public ProjectIndexSyncException(String message) {
        super(message);
    }

    public ProjectIndexSyncException(String message, Throwable cause) {
        super(message, cause);
    }
}
