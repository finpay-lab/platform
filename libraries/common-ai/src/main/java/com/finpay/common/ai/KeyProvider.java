package com.finpay.common.ai;

/**
 * Supplies a BYOK AI key without ever exposing it. The key is redacted from
 * toString()/logging (FP-65 SECURITY). A real impl reads from a secret manager
 * or env; this abstraction lets tests inject a fixed value.
 */
public interface KeyProvider {
    /** Returns the raw key, or null/empty if not configured (caller falls back). */
    String resolve();

    /** Redacted form for logs: never reveals the secret. */
    default String redacted() {
        String k = resolve();
        if (k == null || k.isBlank()) return "<no-key>";
        return "****" + (k.length() <= 4 ? "" : k.substring(k.length() - 4));
    }

    /** Fixed-key provider for local/dev/tests (never logs the literal). */
    static KeyProvider fixed(String key) {
        return () -> key;
    }
}
