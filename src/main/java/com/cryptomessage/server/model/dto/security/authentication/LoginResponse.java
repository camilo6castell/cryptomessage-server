package com.cryptomessage.server.model.dto.security.authentication;

public record LoginResponse(String token, String encryptedPrivateKey, UserResponse user) {

}

