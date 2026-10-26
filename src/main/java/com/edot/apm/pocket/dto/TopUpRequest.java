package com.edot.apm.pocket.dto;

import com.edot.apm.pocket.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class TopUpRequest {

    @NotNull(message = "Top up amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
    private BigDecimal amount;

    private PaymentMethod paymentMethod = PaymentMethod.BANK_TRANSFER;

    private String description;

    public TopUpRequest() {
    }

    public TopUpRequest(BigDecimal amount, PaymentMethod paymentMethod, String description) {
        this.amount = amount;
        this.paymentMethod = (paymentMethod != null) ? paymentMethod : PaymentMethod.BANK_TRANSFER;
        this.description = description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
