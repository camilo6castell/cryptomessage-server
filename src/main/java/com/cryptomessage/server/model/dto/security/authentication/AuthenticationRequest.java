package com.cryptomessage.server.model.dto.security.authentication;

public final class AuthenticationRequest {

    private final String username;
    private final String passphrase;

    public AuthenticationRequest(String username, String passphrase) {
        this.username = username;
        this.passphrase = passphrase;
    }

    public String getUsername() {
        return username;
    }

    public String getPassphrase() {
        return passphrase;
    }
}

