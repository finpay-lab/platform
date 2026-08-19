package com.finpay.common.ai.resilience;

import static org.assertj.core.api.Assertions.assertThat;

import com.finpay.common.ai.resilience.CircuitBreaker.State;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

class CircuitBreakerTest {

    private final AtomicLong now = new AtomicLong(0);

    private CircuitBreaker breaker(int failureThreshold, long openTimeoutMillis, int halfOpenSuccessThreshold) {
        return new CircuitBreaker(failureThreshold, openTimeoutMillis, halfOpenSuccessThreshold, now::get);
    }

    @Test
    void opens_after_the_failure_threshold_and_rejects_requests() {
        CircuitBreaker breaker = breaker(2, 5_000, 1);
        assertThat(breaker.state()).isEqualTo(State.CLOSED);

        breaker.recordFailure();
        assertThat(breaker.state()).isEqualTo(State.CLOSED);
        breaker.recordFailure();

        assertThat(breaker.state()).isEqualTo(State.OPEN);
        assertThat(breaker.tryAcquire()).isFalse();
    }

    @Test
    void a_success_in_closed_state_resets_the_failure_counter() {
        CircuitBreaker breaker = breaker(2, 5_000, 1);
        breaker.recordFailure();
        breaker.recordSuccess();
        breaker.recordFailure();
        breaker.recordFailure();
        assertThat(breaker.state()).isEqualTo(State.OPEN);
    }

    @Test
    void moves_to_half_open_after_cooldown_and_closes_on_success() {
        CircuitBreaker breaker = breaker(2, 5_000, 1);
        breaker.recordFailure();
        breaker.recordFailure();
        assertThat(breaker.tryAcquire()).isFalse();

        now.addAndGet(6_000);
        assertThat(breaker.tryAcquire()).isTrue();
        assertThat(breaker.state()).isEqualTo(State.HALF_OPEN);

        breaker.recordSuccess();
        assertThat(breaker.state()).isEqualTo(State.CLOSED);
        assertThat(breaker.tryAcquire()).isTrue();
    }

    @Test
    void a_failure_in_half_open_reopens_the_circuit() {
        CircuitBreaker breaker = breaker(2, 5_000, 1);
        breaker.recordFailure();
        breaker.recordFailure();
        now.addAndGet(6_000);
        assertThat(breaker.tryAcquire()).isTrue();
        assertThat(breaker.state()).isEqualTo(State.HALF_OPEN);

        breaker.recordFailure();
        assertThat(breaker.state()).isEqualTo(State.OPEN);
        assertThat(breaker.tryAcquire()).isFalse();
    }
}