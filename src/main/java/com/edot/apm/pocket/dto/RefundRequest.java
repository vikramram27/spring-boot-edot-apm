package com.edot.apm.pocket.dto;

import jakarta.validation.constraints.Size;

public class RefundRequest {

    @Size(max = 255, message = "Reason must not exceed 255 characters")
    private String reason;

    public RefundRequest() {
    }

    public RefundRequest(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
