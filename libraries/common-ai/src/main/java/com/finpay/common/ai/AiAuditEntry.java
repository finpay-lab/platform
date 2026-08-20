package com.finpay.common.ai;

import java.time.Instant;

/**
 * Structured audit record for every AI call (FP-65). Deliberately contains NO
 * key, NO raw prompt content beyond a hash, to keep secrets/PII out of logs.
 */
public record AiAuditEntry(
        Instant timestamp,
        String feature,
        String model,
        int promptTokens,
        int completionTokens,
        long latencyMillis,
        boolean success,
        String errorCode) {

    public static AiAuditEntry of(String feature, String model, long latencyMillis,
                                  boolean success, String errorCode) {
        return new AiAuditEntry(Instant.now(), feature, model, 0, 0, latencyMillis, success, errorCode);
    }
}
