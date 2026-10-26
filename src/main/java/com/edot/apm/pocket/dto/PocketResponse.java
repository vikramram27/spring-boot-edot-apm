package com.edot.apm.pocket.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class PocketResponse {

    private final UUID pocketId;
    private final UUID userId;
    private final BigDecimal balance;
    private final String currency;
    private final Instant updatedAt;

    public PocketResponse(UUID pocketId, UUID userId, BigDecimal balance, String currency, Instant updatedAt) {
        this.pocketId = pocketId;
        this.userId = userId;
        this.balance = balance;
        this.currency = currency;
        this.updatedAt = updatedAt;
    }

    public UUID getPocketId() {
        return pocketId;
    }

    public UUID getUserId() {
        return userId;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public String getCurrency() {
        return currency;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
