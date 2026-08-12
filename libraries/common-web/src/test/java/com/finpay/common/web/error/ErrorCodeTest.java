package com.finpay.common.web.error;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ErrorCodeTest {

    @Test
    void every_code_has_a_human_readable_default_message() {
        for (ErrorCode code : ErrorCode.values()) {
            assertThat(code.defaultMessage()).isNotBlank();
        }
    }

    @Test
    void problem_detail_records_status_and_code_without_leaking_internals() {
        var problem = new ProblemDetail(402, "INSUFFICIENT_FUNDS", "Insufficient available balance", "trace-1", null);
        assertThat(problem.status()).isEqualTo(402);
        assertThat(problem.code()).isEqualTo("INSUFFICIENT_FUNDS");
        assertThat(problem.message()).isEqualTo("Insufficient available balance");
    }
}
