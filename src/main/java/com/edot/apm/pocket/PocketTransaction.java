package com.edot.apm.pocket;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pocket_transactions", indexes = {
    @Index(name = "idx_txn_user_created", columnList = "user_id, created_at DESC"),
    @Index(name = "idx_txn_reference", columnList = "reference_id", unique = true),
    @Index(name = "idx_txn_idempotency", columnList = "idempotency_key")
})
public class PocketTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pocket_id", nullable = false)
    private Pocket pocket;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType type;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "balance_after", nullable = false, precision = 15, scale = 2)
    private BigDecimal balanceAfter;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30)
    private PaymentMethod paymentMethod;

    @Column(name = "order_id", length = 64)
    private String orderId;

    @Column(name = "idempotency_key", length = 128)
    private String idempotencyKey;

    @Column(name = "reference_id", nullable = false, unique = true, length = 64)
    private String referenceId;

    @Column(length = 255)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public PocketTransaction() {
    }

    public PocketTransaction(Pocket pocket, UUID userId, TransactionType type, BigDecimal amount,
                             BigDecimal balanceAfter, String currency, TransactionStatus status,
                             PaymentMethod paymentMethod, String orderId, String idempotencyKey,
                             String referenceId, String description) {
        this.pocket = pocket;
        this.userId = userId;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.currency = currency;
        this.status = status;
        this.paymentMethod = paymentMethod;
        this.orderId = orderId;
        this.idempotencyKey = idempotencyKey;
        this.referenceId = referenceId;
        this.description = description;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Pocket getPocket() {
        return pocket;
    }

    public UUID getUserId() {
        return userId;
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

    public String getReferenceId() {
        return referenceId;
    }

    public String getDescription() {
        return description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
