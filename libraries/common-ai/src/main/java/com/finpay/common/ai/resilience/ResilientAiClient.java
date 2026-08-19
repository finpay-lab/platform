package com.finpay.common.ai.resilience;

import com.finpay.common.ai.AiAuditEvent;
import com.finpay.common.ai.AiAuditLogger;
import com.finpay.common.ai.AIClient;
import com.finpay.common.ai.AiCircuitOpenException;
import com.finpay.common.ai.AiException;
import com.finpay.common.ai.ApiKeyProvider;
import com.finpay.common.ai.AiRequest;
import com.finpay.common.ai.AiResponse;
import com.finpay.common.ai.AiStatus;

import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Decorator that adds resilience (AGENTS.md Rules 8 &amp; 9) and audit
 * (SECURITY.md) to any {@link AIClient}: it consults the circuit breaker,
 * retries retryable failures per {@link RetryPolicy}, and emits exactly one
 * structured {@link AiAuditEvent} per logical call — never exposing the raw
 * API key. Pure Java; the delegate holds the transport.
 */
public final class ResilientAiClient implements AIClient {

    private final AIClient delegate;
    private final RetryPolicy retryPolicy;
    private final CircuitBreaker circuitBreaker;
    private final AiAuditLogger auditLogger;
    private final ApiKeyProvider apiKeyProvider;

    public ResilientAiClient(
            AIClient delegate,
            RetryPolicy retryPolicy,
            CircuitBreaker circuitBreaker,
            AiAuditLogger auditLogger,
            ApiKeyProvider apiKeyProvider) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.retryPolicy = Objects.requireNonNull(retryPolicy, "retryPolicy");
        this.circuitBreaker = Objects.requireNonNull(circuitBreaker, "circuitBreaker");
        this.auditLogger = Objects.requireNonNull(auditLogger, "auditLogger");
        this.apiKeyProvider = Objects.requireNonNull(apiKeyProvider, "apiKeyProvider");
    }

    @Override
    public AiResponse complete(AiRequest request) {
        long startedAt = System.nanoTime();
        int attempt = 0;
        while (true) {
            if (!circuitBreaker.tryAcquire()) {
                AiCircuitOpenException blocked = new AiCircuitOpenException(
                        "AI circuit open, rejecting request " + request.requestId() + " (" + request.provider() + ")");
                audit(request, AiStatus.BLOCKED, blocked.code(), attempt, startedAt);
                throw blocked;
            }
            attempt++;
            try {
                AiResponse response = delegate.complete(request);
                circuitBreaker.recordSuccess();
                audit(request, AiStatus.SUCCESS, null, attempt, startedAt);
                return response;
            } catch (AiException e) {
                circuitBreaker.recordFailure();
                if (!retryPolicy.shouldRetry(attempt, e)) {
                    AiStatus status = e.retryable() ? AiStatus.RETRY_EXHAUSTED : AiStatus.FAILED;
                    audit(request, status, e.code(), attempt, startedAt);
                    throw e;
                }
                sleep(retryPolicy.backoffMillis(attempt));
            } catch (RuntimeException e) {
                circuitBreaker.recordFailure();
                audit(request, AiStatus.FAILED, "AI_UNKNOWN", attempt, startedAt);
                throw e;
            }
        }
    }

    private void audit(AiRequest request, AiStatus status, String errorCode, int attempts, long startedAtNanos) {
        auditLogger.record(new AiAuditEvent(
                Instant.now(),
                request.requestId(),
                request.provider(),
                request.model(),
                status,
                errorCode,
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAtNanos),
                attempts,
                apiKeyProvider.masked()));
    }

    private static void sleep(long millis) {
        if (millis <= 0) {
            return;
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for AI retry backoff", e);
        }
    }
}