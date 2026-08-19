package com.finpay.common.ai;

/** Terminal outcome of one logical AI call, recorded in the audit trail. */
public enum AiStatus {
    /** Call completed and returned a response. */
    SUCCESS,
    /** Call failed on a non-retryable error. */
    FAILED,
    /** Call failed because every retryable attempt was exhausted. */
    RETRY_EXHAUSTED,
    /** Call was rejected without reaching the provider (circuit breaker open). */
    BLOCKED
}