package com.finpay.common.ai.resilience;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.finpay.common.ai.AiAuthenticationException;
import com.finpay.common.ai.AiTimeoutException;
import org.junit.jupiter.api.Test;

class RetryPolicyTest {

    @Test
    void stops_retrying_at_max_attempts() {
        RetryPolicy policy = new RetryPolicy(3, 0, 0, 1.0);
        assertThat(policy.shouldRetry(1, new AiTimeoutException("x"))).isTrue();
        assertThat(policy.shouldRetry(2, new AiTimeoutException("x"))).isTrue();
        assertThat(policy.shouldRetry(3, new AiTimeoutException("x"))).isFalse();
    }

    @Test
    void only_retries_retryable_errors() {
        RetryPolicy policy = new RetryPolicy(3, 0, 0, 1.0);
        assertThat(policy.shouldRetry(1, new AiAuthenticationException("x"))).isFalse();
    }

    @Test
    void caps_backoff_at_the_maximum() {
        RetryPolicy policy = new RetryPolicy(5, 100, 150, 2.0);
        assertThat(policy.backoffMillis(1)).isEqualTo(100);
        assertThat(policy.backoffMillis(2)).isEqualTo(150);
        assertThat(policy.backoffMillis(3)).isEqualTo(150);
    }

    @Test
    void rejects_invalid_configuration() {
        assertThatThrownBy(() -> new RetryPolicy(0, 0, 0, 1.0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RetryPolicy(3, 100, 50, 1.0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RetryPolicy(3, 0, 0, 0.5))
                .isInstanceOf(IllegalArgumentException.class);
    }
}