package com.edot.apm.pocket.dto;

import com.edot.apm.pocket.PaymentMethod;
import com.edot.apm.pocket.PocketTransaction;
import com.edot.apm.pocket.TransactionStatus;
import com.edot.apm.pocket.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class TransactionResponse {

    private final UUID id;
    private final String referenceId;
    private final TransactionType type;
    private final BigDecimal amount;
    private final BigDecimal balanceAfter;
    private final String currency;
    private final TransactionStatus status;
    private final PaymentMethod paymentMethod;
    private final String orderId;
    private final String idempotencyKey;
    private final String description;
    private final Instant createdAt;

    public TransactionResponse(UUID id, String referenceId, TransactionType type, BigDecimal amount,
                               BigDecimal balanceAfter, String currency, TransactionStatus status,
                               PaymentMethod paymentMethod, String orderId, String idempotencyKey,
                               String description, Instant createdAt) {
        this.id = id;
        this.referenceId = referenceId;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.currency = currency;
        this.status = status;
        this.paymentMethod = paymentMethod;
        this.orderId = orderId;
        this.idempotencyKey = idempotencyKey;
        this.description = description;
        this.createdAt = createdAt;
    }

    public static TransactionResponse fromEntity(PocketTransaction txn) {
        return new TransactionResponse(
                txn.getId(),
                txn.getReferenceId(),
                txn.getType(),
                txn.getAmount(),
                txn.getBalanceAfter(),
                txn.getCurrency(),
                txn.getStatus(),
                txn.getPaymentMethod(),
                txn.getOrderId(),
                txn.getIdempotencyKey(),
                txn.getDescription(),
                txn.getCreatedAt()
        );
    }

    public UUID getId() {
        return id;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public TransactionType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public String getCurrency() {
        return currency;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getDescription() {
        return description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
