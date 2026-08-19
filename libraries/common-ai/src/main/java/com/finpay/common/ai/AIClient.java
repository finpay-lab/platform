package com.finpay.common.ai;

/**
 * Domain port for calling an AI model provider (AI-8). Services depend on this
 * interface; transport implementations (e.g. {@code HttpAiClient}) live in
 * {@code com.finpay.common.ai.infrastructure}, keeping service domains free of
 * Spring/HTTP imports (AGENTS.md Rule 4).
 */
@FunctionalInterface
public interface AIClient {

    /**
     * Sends one completion request and returns the model output.
     *
     * @throws AiException on any provider-side failure; subclasses carry a
     *                     stable {@code code()} and whether the error is retryable
     */
    AiResponse complete(AiRequest request) throws AiException;
}