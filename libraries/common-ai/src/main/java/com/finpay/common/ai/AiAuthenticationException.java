package com.finpay.common.ai;

/** The API key was rejected by the provider (HTTP 401/403) or is not configured. */
public final class AiAuthenticationException extends AiException {

    public AiAuthenticationException(String message) {
        super("AI_AUTHENTICATION", false, message);
    }

    public AiAuthenticationException(String message, Throwable cause) {
        super("AI_AUTHENTICATION", false, message, cause);
    }
}