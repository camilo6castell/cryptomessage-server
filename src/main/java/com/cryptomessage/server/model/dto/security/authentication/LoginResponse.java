package com.cryptomessage.server.model.dto.security.authentication;

public final class LoginResponse {

    private final String token;
    private final String encryptedPrivateKey;
    private final UserResponse user;

    public LoginResponse(
            String token,
            String encryptedPrivateKey,
            UserResponse user
    ) {
        this.token = token;
        this.encryptedPrivateKey = encryptedPrivateKey;
        this.user = user;
    }

    public String getToken() {
        return token;
    }

    public String getEncryptedPrivateKey() {
        return encryptedPrivateKey;
    }

    public UserResponse getUser() {
        return user;
    }
}

