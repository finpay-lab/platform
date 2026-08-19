package com.finpay.common.ai.resilience;

import java.util.function.LongSupplier;

/**
 * Circuit breaker for AI provider calls (AGENTS.md Rules 8 &amp; 9). Legal
 * transitions are enforced: CLOSED -> OPEN (failure threshold reached),
 * OPEN -> HALF_OPEN (cooldown elapsed), HALF_OPEN -> CLOSED (successes) and
 * HALF_OPEN -> OPEN (any failure). No other transition is possible.
 */
public final class CircuitBreaker {

    public enum State {
        CLOSED,
        OPEN,
        HALF_OPEN
    }

    private final int failureThreshold;
    private final long openTimeoutMillis;
    private final int halfOpenSuccessThreshold;
    private final LongSupplier millisSource;

    private State state = State.CLOSED;
    private int consecutiveFailures;
    private int halfOpenSuccesses;
    private long openedAtMillis;

    public CircuitBreaker(int failureThreshold, long openTimeoutMillis, int halfOpenSuccessThreshold) {
        this(failureThreshold, openTimeoutMillis, halfOpenSuccessThreshold, System::currentTimeMillis);
    }

    CircuitBreaker(int failureThreshold, long openTimeoutMillis, int halfOpenSuccessThreshold, LongSupplier millisSource) {
        if (failureThreshold < 1) {
            throw new IllegalArgumentException("failureThreshold must be >= 1");
        }
        if (openTimeoutMillis < 0) {
            throw new IllegalArgumentException("openTimeoutMillis must be >= 0");
        }
        if (halfOpenSuccessThreshold < 1) {
            throw new IllegalArgumentException("halfOpenSuccessThreshold must be >= 1");
        }
        this.failureThreshold = failureThreshold;
        this.openTimeoutMillis = openTimeoutMillis;
        this.halfOpenSuccessThreshold = halfOpenSuccessThreshold;
        this.millisSource = millisSource;
    }

    /** Whether a request may go to the provider right now (transitions OPEN -> HALF_OPEN lazily). */
    public synchronized boolean tryAcquire() {
        if (state == State.OPEN && millisSource.getAsLong() - openedAtMillis >= openTimeoutMillis) {
            state = State.HALF_OPEN;
            halfOpenSuccesses = 0;
        }
        return state != State.OPEN;
    }

    public synchronized void recordSuccess() {
        switch (state) {
            case CLOSED -> consecutiveFailures = 0;
            case HALF_OPEN -> {
                halfOpenSuccesses++;
                if (halfOpenSuccesses >= halfOpenSuccessThreshold) {
                    state = State.CLOSED;
                    consecutiveFailures = 0;
                    halfOpenSuccesses = 0;
                }
            }
            case OPEN -> { /* success while open is ignored (no probe in flight) */ }
        }
    }

    public synchronized void recordFailure() {
        switch (state) {
            case CLOSED -> {
                consecutiveFailures++;
                if (consecutiveFailures >= failureThreshold) {
                    state = State.OPEN;
                    openedAtMillis = millisSource.getAsLong();
                }
            }
            case HALF_OPEN -> {
                halfOpenSuccesses = 0;
                state = State.OPEN;
                openedAtMillis = millisSource.getAsLong();
            }
            case OPEN -> { /* already open */ }
        }
    }

    public synchronized State state() {
        return state;
    }
}