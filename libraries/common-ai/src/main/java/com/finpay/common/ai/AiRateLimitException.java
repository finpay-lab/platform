package com.finpay.common.ai;

/** The provider throttled the request (HTTP 429). Safe to retry. */
public final class AiRateLimitException extends AiException {

    public AiRateLimitException(String message) {
        super("AI_RATE_LIMITED", true, message);
    }

    public AiRateLimitException(String message, Throwable cause) {
        super("AI_RATE_LIMITED", true, message, cause);
    }
}