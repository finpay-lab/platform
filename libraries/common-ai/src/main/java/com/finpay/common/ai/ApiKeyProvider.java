package com.finpay.common.ai;

/**
 * Port for supplying the BYOK API key. The raw key is fetched from the
 * environment/secrets at call time and is never stored on requests or logged
 * (SECURITY.md: no secrets in code).
 */
public interface ApiKeyProvider {

    /**
     * The raw key to place in the Authorization header.
     *
     * @throws AiAuthenticationException when the key is not configured
     */
    String get();

    /**
     * A safe-to-log representation of the key (masked). Implementations must
     * never return the raw key here.
     */
    default String masked() {
        return SecretRedactor.mask(get());
    }
}