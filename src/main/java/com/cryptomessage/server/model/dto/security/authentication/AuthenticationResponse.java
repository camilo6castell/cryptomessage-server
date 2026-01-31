package com.cryptomessage.server.model.dto.security.authentication;

public final class AuthenticationResponse {

    private final String token;
    private final UserResponse user;

    public AuthenticationResponse(String token, UserResponse user) {
        this.token = token;
        this.user = user;
    }

    public String getToken() {
        return token;
    }

    public UserResponse getUser() {
        return user;
    }
}
