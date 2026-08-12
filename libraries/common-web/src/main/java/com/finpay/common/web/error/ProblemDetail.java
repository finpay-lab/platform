package com.finpay.common.web.error;

import java.util.Map;

/** RFC-9457 shaped problem response. Never leaks internal exception text. */
public record ProblemDetail(
        int status,
        String code,
        String message,
        String traceId,
        Map<String, Object> details
) {}
