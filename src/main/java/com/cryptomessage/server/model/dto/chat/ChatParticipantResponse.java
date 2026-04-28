package com.cryptomessage.server.model.dto.chat;

public class ChatParticipantResponse {
    public Long userId;
    public String username;
    public String publicKey;

    public ChatParticipantResponse(Long userId, String username, String publicKey) {
        this.userId = userId;
        this.username = username;
        this.publicKey = publicKey;
    }
}
