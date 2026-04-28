package com.cryptomessage.server.model.dto.chat;

import com.cryptomessage.server.model.dto.security.authentication.UserResponse;
import com.cryptomessage.server.model.entity.chat.ChatStatus;

import java.time.Instant;

public record ChatResponse(
        Long chatId,
        Long initiatedBy,
        ChatStatus status,
        ChatParticipantResponse participant,
        Instant createdAt
) {}

