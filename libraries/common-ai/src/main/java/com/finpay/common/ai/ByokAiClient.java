package com.finpay.common.ai;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * BYOK AI client with retry + circuit-breaker + structured audit (FP-65 / AI-8).
 *
 * - BYOK key via KeyProvider; injected as a Bearer header but NEVER logged
 *   (KeyProvider.redacted() is used for any diagnostics).
 * - Retry with exponential backoff (Rule 8) on timeout/5xx.
 * - Circuit-breaker: opens after N consecutive failures, half-open probe after
 *   a cooldown, closes on success.
 * - Every call emits an AiAuditEntry (feature, latency, success) — no key/PII.
 */
public final class ByokAiClient implements AIClient {

    private final HttpClient http;
    private final KeyProvider keyProvider;
    private final AiAuditSink auditSink;
    private final String endpoint;
    private final Duration timeout;
    private final int maxRetries;
    private final int failureThreshold;
    private final Duration cooldown;

    // Circuit state
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final AtomicLong openedAt = new AtomicLong(0);
    private volatile boolean open = false;

    public ByokAiClient(HttpClient http, KeyProvider keyProvider, AiAuditSink auditSink,
                        String endpoint, Duration timeout, int maxRetries,
                        int failureThreshold, Duration cooldown) {
        this.http = http;
        this.keyProvider = keyProvider;
        this.auditSink = auditSink;
        this.endpoint = endpoint;
        this.timeout = timeout;
        this.maxRetries = maxRetries;
        this.failureThreshold = failureThreshold;
        this.cooldown = cooldown;
    }

    @Override
    public AiResponse complete(AiRequest request) throws AiException {
        long start = System.nanoTime();
        if (open && openedAt.get() + cooldown.toMillis() > System.currentTimeMillis()) {
            audit(false, request, System.nanoTime() - start, "circuit_open");
            throw new AiException("circuit open");
        }
        Exception last = null;
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                AiResponse r = doCall(request);
                onSuccess();
                audit(true, request, System.nanoTime() - start, null);
                return r;
            } catch (Exception ex) {
                last = ex;
                if (attempt < maxRetries) {
                    try { Thread.sleep(backoffMillis(attempt)); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                }
            }
        }
        onFailure();
        audit(false, request, System.nanoTime() - start, "max_retries");
        throw new AiException("ai call failed after " + (maxRetries + 1) + " attempts", last);
    }

    private AiResponse doCall(AiRequest request) throws Exception {
        String body = "{\"model\":\"" + request.model() + "\",\"max_tokens\":" + request.maxTokens()
                + ",\"prompt\":" + jsonEscape(request.prompt()) + "}";
        HttpRequest.Builder b = HttpRequest.newBuilder().uri(URI.create(endpoint))
                .timeout(timeout).header("Content-Type", "application/json");
        String key = keyProvider.resolve();
        if (key != null && !key.isBlank()) b.header("Authorization", "Bearer " + key);
        HttpRequest req = b.POST(HttpRequest.BodyPublishers.ofString(body)).build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 500) throw new IllegalStateException("upstream " + resp.statusCode());
        if (resp.statusCode() >= 400) throw new IllegalStateException("client error " + resp.statusCode());
        return new AiResponse(resp.body(), estimateTokens(request.prompt()), estimateTokens(resp.body()));
    }

    private void onSuccess() {
        consecutiveFailures.set(0);
        open = false;
    }
    private void onFailure() {
        if (consecutiveFailures.incrementAndGet() >= failureThreshold) {
            open = true;
            openedAt.set(System.currentTimeMillis());
        }
    }

    private long backoffMillis(int attempt) {
        return Math.min(1000L * (1L << attempt), 8000L);
    }
    private int estimateTokens(String s) { return s == null ? 0 : Math.max(1, s.length() / 4); }
    private void audit(boolean ok, AiRequest req, long nanos, String code) {
        if (auditSink != null) {
            auditSink.record(AiAuditEntry.of(req.feature(), req.model(), nanos / 1_000_000, ok, code));
        }
    }
    private static String jsonEscape(String s) {
        if (s == null) return "null";
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> sb.append(c);
            }
        }
        return sb.append("\"").toString();
    }
}
