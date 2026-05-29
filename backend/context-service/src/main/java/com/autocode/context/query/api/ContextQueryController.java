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

    @PostMapping("/impact")
    public ApiResponse<QueryResponsePayload> impact(@RequestBody ContextQueryRequest request) {
        // Impact analysis entry routes to call-relation intent for CS-06 call-chain style queries.
        return ApiResponse.ok("query executed", queryOrchestrationService.execute(request, QueryIntent.CALL_RELATION));
    }

    @PostMapping("/traceability")
    public ApiResponse<QueryResponsePayload> traceability(@RequestBody ContextQueryRequest request) {
        // Traceability entry routes to requirement-analysis intent for CS-07 requirement/code backtracking.
        return ApiResponse.ok("query executed", queryOrchestrationService.execute(request, QueryIntent.REQUIREMENT_ANALYSIS));
    }

    @PostMapping("/ask")
    public ApiResponse<QueryResponsePayload> ask(@RequestBody ContextQueryRequest request) {
        // Generic ask route delegates intent detection to QueryIntentResolver.
        return ApiResponse.ok("query executed", queryOrchestrationService.execute(request, null));
    }
}
