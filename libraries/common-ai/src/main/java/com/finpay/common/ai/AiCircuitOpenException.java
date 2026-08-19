package com.finpay.common.ai;

/** The circuit breaker is open; the request was rejected without reaching the provider. */
public final class AiCircuitOpenException extends AiException {

    public AiCircuitOpenException(String message) {
        super("AI_CIRCUIT_OPEN", false, message);
    }

    public AiCircuitOpenException(String message, Throwable cause) {
        super("AI_CIRCUIT_OPEN", false, message, cause);
    }
}