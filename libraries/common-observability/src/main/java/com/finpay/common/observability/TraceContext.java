package com.finpay.common.observability;

/**
 * Immutable W3C trace context propagated across services so logs and spans can
 * be correlated end-to-end (OBSERVABILITY.md). Mirrors OpenTelemetry's
 * {@code traceparent} encoding.
 */
public record TraceContext(String traceId, String spanId, boolean sampled) {

    public static final int TRACE_ID_HEX_LENGTH = 32;
    public static final int SPAN_ID_HEX_LENGTH = 16;

    public TraceContext {
        if (traceId == null || traceId.isBlank()) {
            throw new IllegalArgumentException("traceId must not be blank");
        }
        if (spanId == null || spanId.isBlank()) {
            throw new IllegalArgumentException("spanId must not be blank");
        }
        if (traceId.length() != TRACE_ID_HEX_LENGTH) {
            throw new IllegalArgumentException(
                    "traceId must be " + TRACE_ID_HEX_LENGTH + " hex chars, was " + traceId.length());
        }
        if (spanId.length() != SPAN_ID_HEX_LENGTH) {
            throw new IllegalArgumentException(
                    "spanId must be " + SPAN_ID_HEX_LENGTH + " hex chars, was " + spanId.length());
        }
    }
}