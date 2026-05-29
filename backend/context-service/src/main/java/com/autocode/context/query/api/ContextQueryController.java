package com.autocode.context.query.api;

import com.autocode.context.api.ApiResponse;
import com.autocode.context.query.ContextQueryRequest;
import com.autocode.context.query.QueryIntent;
import com.autocode.context.query.QueryOrchestrationService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/query")
public class ContextQueryController {
    private final QueryOrchestrationService queryOrchestrationService;

    public ContextQueryController(QueryOrchestrationService queryOrchestrationService) {
        this.queryOrchestrationService = queryOrchestrationService;
    }

    @PostMapping("/code")
    public ApiResponse<QueryResponsePayload> code(@RequestBody ContextQueryRequest request) {
        return ApiResponse.ok("query executed", queryOrchestrationService.execute(request, QueryIntent.CODE_LOCATE));
    }

    @PostMapping("/history")
    public ApiResponse<QueryResponsePayload> history(@RequestBody ContextQueryRequest request) {
        return ApiResponse.ok("query executed", queryOrchestrationService.execute(request, QueryIntent.HISTORY_TRACE));
    }

    @PostMapping("/knowledge")
    public ApiResponse<QueryResponsePayload> knowledge(@RequestBody ContextQueryRequest request) {
        return ApiResponse.ok("query executed", queryOrchestrationService.execute(request, QueryIntent.DOCUMENT_TRACE));
    }

    @PostMapping("/ask")
    public ApiResponse<QueryResponsePayload> ask(@RequestBody ContextQueryRequest request) {
        return ApiResponse.ok("query executed", queryOrchestrationService.execute(request, null));
    }
}
