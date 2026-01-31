package com.cryptomessage.server.model.dto.security.authentication;

import java.time.Instant;

public final class UserResponse {

    private final Long userId;
    private final String username;
    private final Instant createdAt;
    private final String publicKey;

    public UserResponse(
            Long userId,
            String username,
            Instant createdAt,
            String publicKey
    ) {
        this.userId = userId;
        this.username = username;
        this.createdAt = createdAt;
        this.publicKey = publicKey;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getPublicKey() {
        return publicKey;
    }
}
