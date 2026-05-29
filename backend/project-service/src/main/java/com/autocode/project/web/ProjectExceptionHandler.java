package com.autocode.project.web;

import com.autocode.project.service.ProjectNameConflictException;
import com.autocode.project.service.ProjectIndexSyncException;
import com.autocode.project.service.ProjectNotFoundException;
import com.autocode.project.web.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class ProjectExceptionHandler {

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class, HttpMessageNotReadableException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleValidationException(Exception exception, HttpServletRequest request) {
        return error("VALIDATION_ERROR", "Request validation failed", request);
    }

    @ExceptionHandler(ProjectNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handleProjectNotFound(ProjectNotFoundException exception, HttpServletRequest request) {
        return error("PROJECT_NOT_FOUND", exception.getMessage(), request);
    }

    @ExceptionHandler(ProjectNameConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiErrorResponse handleProjectNameConflict(ProjectNameConflictException exception, HttpServletRequest request) {
        return error("PROJECT_NAME_CONFLICT", exception.getMessage(), request);
    }

    @ExceptionHandler(ProjectIndexSyncException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleProjectIndexSync(ProjectIndexSyncException exception, HttpServletRequest request) {
        return error("PROJECT_INDEX_SYNC_ERROR", exception.getMessage(), request);
    }

    private ApiErrorResponse error(String code, String message, HttpServletRequest request) {
        return new ApiErrorResponse(code, message, request.getRequestURI(), Instant.now());
    }
}
