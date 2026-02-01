package com.cryptomessage.server.model.dto.chat;

import com.cryptomessage.server.model.dto.security.authentication.UserResponse;
import com.cryptomessage.server.model.entity.chat.ChatStatus;

import java.time.Instant;

public final class ChatResponse {

    private final Long chatId;
    private final ChatStatus status;
    private final UserResponse participant;
    private final Instant createdAt;

    public ChatResponse(
            Long chatId,
            ChatStatus status,
            UserResponse participant,
            Instant createdAt
    ) {
        this.chatId = chatId;
        this.status = status;
        this.participant = participant;
        this.createdAt = createdAt;
    }

    public Long getChatId() {
        return chatId;
    }

    public ChatStatus getStatus() {
        return status;
    }

    public UserResponse getParticipant() {
        return participant;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

