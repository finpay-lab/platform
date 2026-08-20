package com.finpay.common.ai;

/**
 * Domain-port style AI client (FP-65 / AI-8). Hexagonal: services depend on
 * this interface in their domain/ layer; the BYOK impl lives here (infra-free
 * from the consumer's perspective). BYOK key is supplied via KeyProvider and is
 * never part of the prompt or any log line.
 */
public interface AIClient {
    /** Complete a prompt. Throws AiException on transport/AI failure. */
    AiResponse complete(AiRequest request) throws AiException;

    record AiRequest(String feature, String model, String prompt, int maxTokens) {}
    record AiResponse(String content, int promptTokens, int completionTokens) {}
    class AiException extends RuntimeException {
        public AiException(String m) { super(m); }
        public AiException(String m, Throwable c) { super(m, c); }
    }
}
