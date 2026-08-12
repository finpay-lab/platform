package com.finpay.common.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class TraceContextTest {

    @Test
    void accepts_a_valid_w3c_trace_context() {
        var ctx = new TraceContext("0123456789abcdef0123456789abcdef", "0123456789abcdef", true);
        assertThat(ctx.traceId()).hasSize(32);
        assertThat(ctx.spanId()).hasSize(16);
        assertThat(ctx.sampled()).isTrue();
    }

    @Test
    void rejects_blank_trace_id() {
        assertThatThrownBy(() -> new TraceContext(" ", "0123456789abcdef", true))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_malformed_id_lengths() {
        assertThatThrownBy(() -> new TraceContext("short", "0123456789abcdef", true))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TraceContext("0123456789abcdef0123456789abcdef", "x", true))
                .isInstanceOf(IllegalArgumentException.class);
    }
}