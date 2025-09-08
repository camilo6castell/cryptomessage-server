package com.cryptomessage.server.model.dto.security.register;

public class RegisterRequest {
    private String username;
    private String passphrase;

    public RegisterRequest(){}

    public RegisterRequest(String username, String passphrase){
        this.username = username;
        this.passphrase = passphrase;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassphrase() {
        return passphrase;
    }

    public void setPassphrase(String passphrase) {
        this.passphrase = passphrase;
    }
}
