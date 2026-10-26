package com.edot.apm.user.dto;

import com.edot.apm.user.Role;
import java.time.Instant;
import java.util.UUID;

public class UserProfileResponse {

    private final UUID id;
    private final String name;
    private final String email;
    private final Role role;
    private final Instant createdAt;

    public UserProfileResponse(UUID id, String name, String email, Role role, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public Role getRole() {
        return role;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
