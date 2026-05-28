package com.autocode.project.service;

public class ProjectNameConflictException extends RuntimeException {

    public ProjectNameConflictException(String projectName, Throwable cause) {
        super("Project name already exists: " + projectName, cause);
    }
}
