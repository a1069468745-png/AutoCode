package com.autocode.llm.web;

import com.autocode.llm.audit.LlmCallLogger;
import com.autocode.llm.web.dto.ChatCompletionRequest;
import com.autocode.llm.web.dto.ChatCompletionResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LlmChatController {

    private final LlmCallLogger callLogger;

    public LlmChatController(LlmCallLogger callLogger) {
        this.callLogger = callLogger;
    }

    @PostMapping("/v1/chat/completions")
    public ChatCompletionResponse chatCompletions(
            @RequestHeader(value = "X-Project-Id", required = false) Long projectId,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @Valid @RequestBody ChatCompletionRequest request) {

        ChatCompletionResponse response = ChatCompletionResponse.placeholder(request.model(), request.prompt());

        if (projectId != null) {
            callLogger.logCall(
                    projectId,
                    userId != null ? userId : "anonymous",
                    "chat",
                    request.prompt(),
                    request.model(),
                    0
            );
        }

        return response;
    }
}
