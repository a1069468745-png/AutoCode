package com.autocode.project.service;

public class ProjectNotFoundException extends RuntimeException {

    public ProjectNotFoundException(long projectId) {
        super("Project not found: " + projectId);
    }
}
