package com.edot.apm.pocket.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class PaymentChargeRequest {

    @NotNull(message = "Payment amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
    private BigDecimal amount;

    @NotBlank(message = "Order ID is required")
    private String orderId;

    private String description;

    public PaymentChargeRequest() {
    }

    public PaymentChargeRequest(BigDecimal amount, String orderId, String description) {
        this.amount = amount;
        this.orderId = orderId;
        this.description = description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
