package com.finpay.common.ai.resilience;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.finpay.common.ai.AiAuditEvent;
import com.finpay.common.ai.AiAuditLogger;
import com.finpay.common.ai.AIClient;
import com.finpay.common.ai.AiAuthenticationException;
import com.finpay.common.ai.AiCircuitOpenException;
import com.finpay.common.ai.AiRequest;
import com.finpay.common.ai.AiResponse;
import com.finpay.common.ai.AiStatus;
import com.finpay.common.ai.AiTimeoutException;
import com.finpay.common.ai.ApiKeyProvider;
import com.finpay.common.ai.SecretRedactor;
import com.finpay.common.ai.resilience.CircuitBreaker.State;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

class ResilientAiClientTest {

    private static final String REQUEST_ID = "req-1";
    private static final String PROVIDER = "openai";
    private static final String MODEL = "gpt-test";
    private static final String RAW_KEY = "sk-super-secret-1234567890";

    private final AiRequest request = new AiRequest(REQUEST_ID, PROVIDER, MODEL, "Summarize the ledger");

    @Test
    void retries_on_timeout_and_succeeds_with_one_success_audit_event() {
        AIClient delegate = mock(AIClient.class);
        when(delegate.complete(any()))
                .thenThrow(new AiTimeoutException("timeout"))
                .thenThrow(new AiTimeoutException("timeout"))
                .thenReturn(new AiResponse(REQUEST_ID, MODEL, "ok", 5));

        RecordingAuditLogger audit = new RecordingAuditLogger();
        ResilientAiClient client = client(delegate, new RetryPolicy(3, 0, 0, 1.0), new CircuitBreaker(5, 1_000, 1), audit);

        AiResponse response = client.complete(request);

        assertThat(response.content()).isEqualTo("ok");
        verify(delegate, times(3)).complete(request);
        assertThat(audit.events).hasSize(1);
        AiAuditEvent event = audit.events.get(0);
        assertThat(event.status()).isEqualTo(AiStatus.SUCCESS);
        assertThat(event.attempts()).isEqualTo(3);
        assertThat(event.requestId()).isEqualTo(REQUEST_ID);
        assertThat(event.provider()).isEqualTo(PROVIDER);
        assertThat(event.model()).isEqualTo(MODEL);
        assertThat(event.latencyMillis()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void throws_after_retries_are_exhausted_and_emits_retry_exhausted_event() {
        AIClient delegate = mock(AIClient.class);
        when(delegate.complete(any())).thenThrow(new AiTimeoutException("timeout"));

        RecordingAuditLogger audit = new RecordingAuditLogger();
        ResilientAiClient client = client(delegate, new RetryPolicy(3, 0, 0, 1.0), new CircuitBreaker(5, 1_000, 1), audit);

        assertThatThrownBy(() -> client.complete(request)).isInstanceOf(AiTimeoutException.class);
        verify(delegate, times(3)).complete(request);

        assertThat(audit.events).hasSize(1);
        AiAuditEvent event = audit.events.get(0);
        assertThat(event.status()).isEqualTo(AiStatus.RETRY_EXHAUSTED);
        assertThat(event.attempts()).isEqualTo(3);
        assertThat(event.errorCode()).isEqualTo("AI_TIMEOUT");
    }

    @Test
    void does_not_retry_non_retryable_errors() {
        AIClient delegate = mock(AIClient.class);
        when(delegate.complete(any())).thenThrow(new AiAuthenticationException("rejected"));

        RecordingAuditLogger audit = new RecordingAuditLogger();
        ResilientAiClient client = client(delegate, new RetryPolicy(3, 0, 0, 1.0), new CircuitBreaker(5, 1_000, 1), audit);

        assertThatThrownBy(() -> client.complete(request)).isInstanceOf(AiAuthenticationException.class);
        verify(delegate, times(1)).complete(request);

        assertThat(audit.events).hasSize(1);
        assertThat(audit.events.get(0).status()).isEqualTo(AiStatus.FAILED);
        assertThat(audit.events.get(0).attempts()).isEqualTo(1);
        assertThat(audit.events.get(0).errorCode()).isEqualTo("AI_AUTHENTICATION");
    }

    @Test
    void blocks_without_calling_the_provider_when_the_circuit_is_open() {
        AIClient delegate = mock(AIClient.class);
        when(delegate.complete(any())).thenThrow(new AiTimeoutException("timeout"));

        CircuitBreaker breaker = new CircuitBreaker(1, 60_000, 1);
        RecordingAuditLogger audit = new RecordingAuditLogger();
        ResilientAiClient client = client(delegate, new RetryPolicy(3, 0, 0, 1.0), breaker, audit);

        assertThatThrownBy(() -> client.complete(request)).isInstanceOf(AiCircuitOpenException.class);
        assertThat(breaker.state()).isEqualTo(State.OPEN);
        assertThat(audit.events).hasSize(1);
        assertThat(audit.events.get(0).status()).isEqualTo(AiStatus.BLOCKED);

        assertThatThrownBy(() -> client.complete(request)).isInstanceOf(AiCircuitOpenException.class);
        verify(delegate, times(1)).complete(request);
    }

    @Test
    void audit_event_never_contains_the_raw_api_key() {
        AIClient delegate = mock(AIClient.class);
        when(delegate.complete(any())).thenReturn(new AiResponse(REQUEST_ID, MODEL, "ok", 5));

        RecordingAuditLogger audit = new RecordingAuditLogger();
        ResilientAiClient client = client(delegate, new RetryPolicy(3, 0, 0, 1.0), new CircuitBreaker(5, 1_000, 1), audit);

        client.complete(request);

        AiAuditEvent event = audit.events.get(0);
        assertThat(event.maskedApiKey()).isEqualTo(SecretRedactor.mask(RAW_KEY));
        assertThat(event.maskedApiKey()).doesNotContain(RAW_KEY);
    }

    private ResilientAiClient client(
            AIClient delegate, RetryPolicy retryPolicy, CircuitBreaker circuitBreaker, RecordingAuditLogger audit) {
        return new ResilientAiClient(delegate, retryPolicy, circuitBreaker, audit, new FixedKeyProvider(RAW_KEY));
    }

    private static final class FixedKeyProvider implements ApiKeyProvider {
        private final String key;

        FixedKeyProvider(String key) {
            this.key = key;
        }

        @Override
        public String get() {
            return key;
        }
    }

    private static final class RecordingAuditLogger implements AiAuditLogger {
        final List<AiAuditEvent> events = new ArrayList<>();

        @Override
        public void record(AiAuditEvent event) {
            events.add(event);
        }
    }
}