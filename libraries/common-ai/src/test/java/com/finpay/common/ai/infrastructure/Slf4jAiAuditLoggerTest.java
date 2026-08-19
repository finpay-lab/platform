package com.finpay.common.ai.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.finpay.common.ai.AiAuditEvent;
import com.finpay.common.ai.AiStatus;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;

class Slf4jAiAuditLoggerTest {

    private static final String RAW_KEY = "sk-ultra-secret-value-abcdef";
    private static final String MASKED_KEY = "****cdef";

    @Test
    void structured_log_never_contains_the_raw_api_key() {
        Logger logger = (Logger) LoggerFactory.getLogger(Slf4jAiAuditLogger.LOGGER_NAME);
        logger.setLevel(Level.INFO);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            new Slf4jAiAuditLogger().record(new AiAuditEvent(
                    Instant.parse("2026-08-19T10:00:00Z"), "req-1", "openai", "gpt-test",
                    AiStatus.SUCCESS, null, 42, 1, MASKED_KEY));

            List<String> messages = appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
            assertThat(messages).isNotEmpty();
            for (String message : messages) {
                assertThat(message).contains("audit.ai.call");
                assertThat(message).contains("requestId=req-1");
                assertThat(message).contains("status=SUCCESS");
                assertThat(message).contains(MASKED_KEY);
                assertThat(message).doesNotContain(RAW_KEY);
            }
        } finally {
            logger.detachAppender(appender);
        }
    }
}