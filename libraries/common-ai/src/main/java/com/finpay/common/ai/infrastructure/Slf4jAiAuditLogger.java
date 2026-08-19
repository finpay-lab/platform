package com.finpay.common.ai.infrastructure;

import com.finpay.common.ai.AiAuditEvent;
import com.finpay.common.ai.AiAuditLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Structured SLF4J audit logger for AI calls. Emits a single key=value line per
 * event under a dedicated logger name so it can be routed to an append-only
 * audit sink. Only the masked key fragment is ever logged.
 */
public final class Slf4jAiAuditLogger implements AiAuditLogger {

    public static final String LOGGER_NAME = "com.finpay.ai.audit";

    private static final Logger LOG = LoggerFactory.getLogger(LOGGER_NAME);

    @Override
    public void record(AiAuditEvent event) {
        LOG.info(
                "audit.ai.call requestId={} provider={} model={} status={} errorCode={} latencyMs={} attempts={} maskedApiKey={}",
                event.requestId(),
                event.provider(),
                event.model(),
                event.status(),
                event.errorCode() == null ? "-" : event.errorCode(),
                event.latencyMillis(),
                event.attempts(),
                event.maskedApiKey() == null ? "-" : event.maskedApiKey());
    }
}