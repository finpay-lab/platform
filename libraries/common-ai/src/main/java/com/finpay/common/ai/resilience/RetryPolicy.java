package com.finpay.common.ai.resilience;

import com.finpay.common.ai.AiException;

/**
 * Backoff/attempt policy for retryable AI failures (AGENTS.md Rule 8).
 * Retries are capped at {@code maxAttempts}; only retryable {@link AiException}
 * subtypes are retried, so hard failures (auth, bad input) fail fast.
 */
public record RetryPolicy(int maxAttempts, long initialBackoffMillis, long maxBackoffMillis, double backoffMultiplier) {

    public static final int DEFAULT_MAX_ATTEMPTS = 3;
    public static final long DEFAULT_INITIAL_BACKOFF_MILLIS = 100;
    public static final long DEFAULT_MAX_BACKOFF_MILLIS = 1_000;
    public static final double DEFAULT_BACKOFF_MULTIPLIER = 2.0;

    public RetryPolicy {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be >= 1");
        }
        if (initialBackoffMillis < 0) {
            throw new IllegalArgumentException("initialBackoffMillis must be >= 0");
        }
        if (maxBackoffMillis < initialBackoffMillis) {
            throw new IllegalArgumentException("maxBackoffMillis must be >= initialBackoffMillis");
        }
        if (backoffMultiplier < 1.0) {
            throw new IllegalArgumentException("backoffMultiplier must be >= 1.0");
        }
    }

    public static RetryPolicy defaults() {
        return new RetryPolicy(
                DEFAULT_MAX_ATTEMPTS,
                DEFAULT_INITIAL_BACKOFF_MILLIS,
                DEFAULT_MAX_BACKOFF_MILLIS,
                DEFAULT_BACKOFF_MULTIPLIER);
    }

    /** True when {@code attempt} (1-based) may be retried and the error is retryable. */
    public boolean shouldRetry(int attempt, Throwable error) {
        return attempt < maxAttempts && error instanceof AiException ai && ai.retryable();
    }

    /** Exponential backoff for a given 1-based attempt, capped at {@code maxBackoffMillis}. */
    public long backoffMillis(int attempt) {
        long backoff = initialBackoffMillis;
        for (int i = 1; i < attempt; i++) {
            backoff = Math.min(maxBackoffMillis, (long) (backoff * backoffMultiplier));
        }
        return backoff;
    }
}