package com.finpay.common.ai;

/**
 * Base for all AI-provider failures. Each subtype carries a stable error code
 * (used in audit events) and a retryability flag consumed by
 * {@code com.finpay.common.ai.resilience.RetryPolicy}.
 */
public abstract class AiException extends RuntimeException {

    private final String code;
    private final boolean retryable;

    protected AiException(String code, boolean retryable, String message) {
        super(message);
        this.code = code;
        this.retryable = retryable;
    }

    protected AiException(String code, boolean retryable, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.retryable = retryable;
    }

    public final String code() {
        return code;
    }

    public final boolean retryable() {
        return retryable;
    }
}