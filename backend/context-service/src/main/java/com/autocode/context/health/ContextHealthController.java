package com.autocode.context.health;

import com.autocode.context.api.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/internal/context")
public class ContextHealthController {
    private final ContextReadinessService contextReadinessService;

    public ContextHealthController(ContextReadinessService contextReadinessService) {
        this.contextReadinessService = contextReadinessService;
    }

    @GetMapping("/health")
    public ApiResponse<Map<String, String>> health() {
        return ApiResponse.ok("context-service is healthy", Map.of("status", "UP"));
    }

    @GetMapping("/readiness")
    public ResponseEntity<ApiResponse<ContextReadiness>> readiness() {
        ContextReadiness readiness = contextReadinessService.check();
        HttpStatus status = "UP".equals(readiness.status()) ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(status)
                .body(ApiResponse.ok("context-service readiness checked", readiness));
    }
}
