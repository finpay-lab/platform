package com.finpay.common.ai;

/**
 * Redacts secrets for logs and audit events. The raw key is never written to
 * any output; only a masked suffix may be exposed.
 */
public final class SecretRedactor {

    public static final String MASKED_UNSET = "(unset)";
    private static final String MASK = "****";

    private SecretRedactor() {
    }

    public static String mask(String secret) {
        if (secret == null || secret.isBlank()) {
            return MASKED_UNSET;
        }
        if (secret.length() <= 4) {
            return MASK;
        }
        return MASK + secret.substring(secret.length() - 4);
    }
}