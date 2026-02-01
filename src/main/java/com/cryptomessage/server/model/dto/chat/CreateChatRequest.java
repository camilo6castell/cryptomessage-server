package com.cryptomessage.server.model.dto.chat;

public class CreateChatRequest {

    private String username;

    public CreateChatRequest() {}

    public CreateChatRequest(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }
}


