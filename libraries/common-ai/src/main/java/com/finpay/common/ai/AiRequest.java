package com.finpay.common.ai;

import java.util.Objects;
import java.util.UUID;

/**
 * A completion request for an AI model provider. Immutable; carries a caller
 * supplied {@code requestId} so audit events and service idempotency can be
 * correlated. The API key is never part of the request — it lives only in the
 * transport layer.
 */
public record AiRequest(String requestId, String provider, String model, String prompt) {

    public AiRequest {
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }
        if (provider == null || provider.isBlank()) {
            throw new IllegalArgumentException("provider must not be blank");
        }
        if (model == null || model.isBlank()) {
            throw new IllegalArgumentException("model must not be blank");
        }
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("prompt must not be blank");
        }
    }
}