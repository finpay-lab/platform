package com.finpay.common.ai;

/** Sink for AI audit entries (Rule 8 observability; SECURITY: no key/PII). */
public interface AiAuditSink {
    void record(AiAuditEntry entry);
}
