package com.finpay.common.ai;

import java.time.Instant;

/**
 * Structured record of one logical AI call: WHO (request id), WHAT (model),
 * WHEN (timestamp), RESULT (status + error code), plus latency, attempt count
 * and a masked key fragment. Never carries the raw API key.
 */
public record AiAuditEvent(
        Instant timestamp,
        String requestId,
        String provider,
        String model,
        AiStatus status,
        String errorCode,
        long latencyMillis,
        int attempts,
        String maskedApiKey) {

    public AiAuditEvent {
        if (requestId == null || requestId.isBlank()) {
            throw new IllegalArgumentException("requestId must not be blank");
        }
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }
    }
}