package com.finpay.common.ai;

/**
 * Port for the AI audit trail (SECURITY.md audit section). Every AI call must
 * emit exactly one structured event after it completes (or is blocked).
 */
@FunctionalInterface
public interface AiAuditLogger {

    void record(AiAuditEvent event);
}