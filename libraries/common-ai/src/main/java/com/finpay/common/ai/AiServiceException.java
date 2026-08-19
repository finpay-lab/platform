package com.finpay.common.ai;

/** The provider returned a transient 5xx error or an unusable payload. */
public final class AiServiceException extends AiException {

    public AiServiceException(String message) {
        super("AI_SERVICE_ERROR", true, message);
    }

    public AiServiceException(String message, Throwable cause) {
        super("AI_SERVICE_ERROR", true, message, cause);
    }
}