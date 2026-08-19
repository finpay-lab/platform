package com.finpay.common.ai;

/** The provider did not answer within the configured read/connect timeout. */
public final class AiTimeoutException extends AiException {

    public AiTimeoutException(String message) {
        super("AI_TIMEOUT", true, message);
    }

    public AiTimeoutException(String message, Throwable cause) {
        super("AI_TIMEOUT", true, message, cause);
    }
}