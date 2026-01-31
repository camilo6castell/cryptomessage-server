package com.cryptomessage.server.model.dto.security.register;

public final class RegisterRequest {

    private final String username;
    private final String passphrase;

    public RegisterRequest(String username, String passphrase) {
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

