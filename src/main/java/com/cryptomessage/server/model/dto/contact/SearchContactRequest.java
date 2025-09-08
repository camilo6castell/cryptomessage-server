package com.cryptomessage.server.model.dto.contact;

public class SearchContactRequest {
    private String username;

    public SearchContactRequest() {
    }

    public SearchContactRequest(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
