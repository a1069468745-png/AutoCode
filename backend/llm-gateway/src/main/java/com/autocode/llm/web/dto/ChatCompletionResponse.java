package com.autocode.llm.web.dto;

public record ChatCompletionResponse(
        String model,
        String content,
        String status,
        long promptTokens,
        long completionTokens
) {
    public static ChatCompletionResponse placeholder(String model, String prompt) {
        return new ChatCompletionResponse(
                model,
                "[LLM Gateway placeholder] received prompt: " + prompt,
                "placeholder",
                0,
                0
        );
    }
}
