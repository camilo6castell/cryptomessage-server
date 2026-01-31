package com.cryptomessage.server.model.dto.security.authentication;

public final class VerifyTokenRequest {

    private final String token;

    public VerifyTokenRequest(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }
}

