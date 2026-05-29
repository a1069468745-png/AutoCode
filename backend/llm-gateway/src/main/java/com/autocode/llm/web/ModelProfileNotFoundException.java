package com.autocode.llm.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ModelProfileNotFoundException extends RuntimeException {

    public ModelProfileNotFoundException(long projectId) {
        super("model profile not found for project " + projectId);
    }
}
