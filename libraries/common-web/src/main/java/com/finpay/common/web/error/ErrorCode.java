package com.finpay.common.web.error;

/** Stable machine-readable error codes returned by the platform API. */
public enum ErrorCode {
    INSUFFICIENT_FUNDS("Insufficient available balance"),
    ACCOUNT_NOT_FOUND("Referenced account does not exist"),
    CUSTOMER_NOT_FOUND("Referenced customer does not exist"),
    TRANSFER_NOT_FOUND("Referenced transfer does not exist"),
    INVALID_STATE_TRANSITION("The requested state transition is not allowed"),
    IDEMPOTENCY_CONFLICT("Request with this idempotency key already exists with a different payload"),
    RATE_LIMITED("Too many requests"),
    RISK_REJECTED("Transaction rejected by risk evaluation"),
    LIMIT_EXCEEDED("Transaction limit exceeded"),
    INTERNAL_ERROR("An unexpected error occurred");

    private final String defaultMessage;

    ErrorCode(String defaultMessage) {
        this.defaultMessage = defaultMessage;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
