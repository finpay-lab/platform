package com.finpay.common.ai;

/**
 * The model's reply to an {@link AiRequest}. Carries the measured latency so
 * audit/Observability can track AI call duration. Never contains the API key.
 */
public record AiResponse(String requestId, String model, String content, long latencyMillis) {

    public AiResponse {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content must not be blank");
        }
    }
}