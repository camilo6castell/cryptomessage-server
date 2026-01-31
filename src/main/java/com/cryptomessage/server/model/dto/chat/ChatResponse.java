package com.cryptomessage.server.model.dto.chat;

import com.cryptomessage.server.model.dto.security.authentication.UserResponse;

import java.time.Instant;

public final class ChatResponse {

    private final Long chatId;
    private final UserResponse participant;
    private final Instant createdAt;

    public ChatResponse(
            Long chatId,
            UserResponse participant,
            Instant createdAt
    ) {
        this.chatId = chatId;
        this.participant = participant;
        this.createdAt = createdAt;
    }

    public Long getChatId() {
        return chatId;
    }

    public UserResponse getParticipant() {
        return participant;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
