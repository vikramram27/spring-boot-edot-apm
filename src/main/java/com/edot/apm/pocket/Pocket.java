package com.edot.apm.pocket;

import com.edot.apm.common.exception.InsufficientBalanceException;
import com.edot.apm.user.User;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pockets", indexes = {
    @Index(name = "idx_pocket_user", columnList = "user_id", unique = true)
})
public class Pocket {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal balance;

    @Column(nullable = false, length = 3)
    private String currency;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Pocket() {
    }

    public Pocket(User user, String currency) {
        this.user = user;
        this.currency = (currency != null && !currency.isBlank()) ? currency.toUpperCase() : "USD";
        this.balance = BigDecimal.ZERO.setScale(2);
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        if (this.balance == null) {
            this.balance = BigDecimal.ZERO.setScale(2);
        }
        if (this.currency == null) {
            this.currency = "USD";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void credit(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Credit amount must be greater than zero");
        }
        this.balance = this.balance.add(amount).setScale(2);
    }

    public void debit(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Debit amount must be greater than zero");
        }
        if (this.balance.compareTo(amount) < 0) {
            throw new InsufficientBalanceException(
                    String.format("Insufficient pocket balance. Required: %s %s, Available: %s %s",
                            this.currency, amount, this.currency, this.balance)
            );
        }
        this.balance = this.balance.subtract(amount).setScale(2);
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public String getCurrency() {
        return currency;
    }

    public Long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
