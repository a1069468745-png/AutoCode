package com.autocode.gateway.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

@Component
public class ProjectPermissionFilter extends OncePerRequestFilter {
    private static final String PROJECT_HEADER = "X-Project-Id";

    private final ProjectAccessEvaluator projectAccessEvaluator;
    private final ObjectMapper objectMapper;

    public ProjectPermissionFilter(ProjectAccessEvaluator projectAccessEvaluator, ObjectMapper objectMapper) {
        this.projectAccessEvaluator = projectAccessEvaluator;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!request.getRequestURI().startsWith("/api/query/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String projectIdHeader = request.getHeader(PROJECT_HEADER);
        if (projectIdHeader == null || projectIdHeader.isBlank()) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, "PROJECT_ID_REQUIRED", "X-Project-Id header is required");
            return;
        }

        long projectId;
        try {
            projectId = Long.parseLong(projectIdHeader);
        } catch (NumberFormatException ex) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, "PROJECT_ID_INVALID", "X-Project-Id must be a number");
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication == null ? "" : ((DevPrincipal) authentication.getPrincipal()).userId();
        // Enforce project-level ownership before request enters downstream query chain.
        if (!projectAccessEvaluator.isAllowed(projectId, userId)) {
            writeError(response, HttpServletResponse.SC_FORBIDDEN, "FORBIDDEN_PROJECT", "user is not authorized for the project");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void writeError(HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), Map.of("code", code, "message", message));
    }
}
