package com.cryptomessage.server.model.dto.security.authentication;

import java.time.Instant;

public record UserResponse(Long userId, String username, Instant createdAt, String publicKey, String encryptedPrivateKey) {

}
