package com.finpay.common.ai.infrastructure;

import com.finpay.common.ai.AiAuthenticationException;
import com.finpay.common.ai.ApiKeyProvider;
import com.finpay.common.ai.SecretRedactor;

import java.util.Objects;

/**
 * Reads the BYOK API key from an environment variable at call time
 * (SECURITY.md: real values come from env/Kubernetes Secrets, never Git).
 * The key is never logged; error messages only name the variable.
 */
public final class EnvApiKeyProvider implements ApiKeyProvider {

    public static final String DEFAULT_ENV_VAR = "FINPAY_AI_API_KEY";

    private final String envVar;

    public EnvApiKeyProvider() {
        this(DEFAULT_ENV_VAR);
    }

    public EnvApiKeyProvider(String envVar) {
        if (envVar == null || envVar.isBlank()) {
            throw new IllegalArgumentException("envVar must not be blank");
        }
        this.envVar = envVar;
    }

    @Override
    public String get() {
        String key = System.getenv(envVar);
        if (key == null || key.isBlank()) {
            throw new AiAuthenticationException("AI API key not configured; set environment variable " + envVar);
        }
        return key;
    }

    @Override
    public String masked() {
        String key = System.getenv(envVar);
        return (key == null || key.isBlank()) ? SecretRedactor.MASKED_UNSET : SecretRedactor.mask(key);
    }

    public String envVar() {
        return envVar;
    }
}