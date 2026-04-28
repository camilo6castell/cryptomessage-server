package com.cryptomessage.server.model.dto.security.authentication;

import java.time.Instant;

public record UserResponse(
        Long userId,
        String username,
        String token,
        String publicKey,
        String encryptedPrivateKey,
        Instant createdAt
) {}
