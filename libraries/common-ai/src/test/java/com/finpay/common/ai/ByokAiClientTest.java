package com.finpay.common.ai;

import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class ByokAiClientTest {

    static final class CapturingSink implements AiAuditSink {
        final List<AiAuditEntry> entries = new ArrayList<>();
        @Override public void record(AiAuditEntry e) { entries.add(e); }
    }

    private ByokAiClient client(AiAuditSink sink, int maxRetries) {
        return new ByokAiClient(HttpClient.newHttpClient(),
                KeyProvider.fixed("sk-SUPERSECRET1234"), sink,
                "http://127.0.0.1:1/none", Duration.ofMillis(200), maxRetries, 3, Duration.ofMillis(50));
    }

    @Test
    void keyIsRedactedAndNeverLeaked() {
        KeyProvider kp = KeyProvider.fixed("sk-SUPERSECRET1234");
        assertThat(kp.redacted()).doesNotContain("SUPERSECRET").contains("****");
        assertThat(kp.redacted()).isEqualTo("****1234");
    }

    @Test
    void auditEmittedOnFailureWithNoKey() {
        CapturingSink sink = new CapturingSink();
        ByokAiClient c = client(sink, 1);
        try { c.complete(new AIClient.AiRequest("explain", "gpt", "hi", 16)); }
        catch (AIClient.AiException ignored) {}
        assertThat(sink.entries).isNotEmpty();
        AiAuditEntry e = sink.entries.get(0);
        assertThat(e.feature()).isEqualTo("explain");
        assertThat(e.success()).isFalse();
        // Audit entry must not contain the secret
        assertThat(e.toString()).doesNotContain("SUPERSECRET").doesNotContain("sk-");
    }

    @Test
    void retriesThenOpensCircuitAfterThreshold() {
        CapturingSink sink = new CapturingSink();
        // endpoint unreachable -> each call fails; failureThreshold=2
        ByokAiClient c = new ByokAiClient(HttpClient.newHttpClient(),
                KeyProvider.fixed("sk-abc123"), sink,
                "http://127.0.0.1:1/none", Duration.ofMillis(100), 0, 2, Duration.ofMillis(50));
        AtomicInteger attempts = new AtomicInteger(0);
        for (int i = 0; i < 5; i++) {
            try { c.complete(new AIClient.AiRequest("t", "m", "p", 4)); }
            catch (AIClient.AiException ex) { attempts.incrementAndGet(); }
        }
        assertThat(attempts.get()).isEqualTo(5);
        // once circuit opens, remaining calls fail fast (audit still emitted, errorCode circuit_open)
        assertThat(sink.entries).extracting(AiAuditEntry::errorCode)
                .contains("circuit_open");
    }
}
