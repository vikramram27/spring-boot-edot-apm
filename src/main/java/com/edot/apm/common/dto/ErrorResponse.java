package com.edot.apm.common.dto;

import java.time.Instant;
import java.util.Map;

public class ErrorResponse {

    private final boolean success = false;
    private final int status;
    private final String error;
    private final String message;
    private final Map<String, String> validationErrors;
    private final Instant timestamp;

    public ErrorResponse(int status, String error, String message) {
        this(status, error, message, null);
    }

    public ErrorResponse(int status, String error, String message, Map<String, String> validationErrors) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.validationErrors = validationErrors;
        this.timestamp = Instant.now();
    }

    public boolean isSuccess() {
        return success;
    }

    public int getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    public Map<String, String> getValidationErrors() {
        return validationErrors;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
