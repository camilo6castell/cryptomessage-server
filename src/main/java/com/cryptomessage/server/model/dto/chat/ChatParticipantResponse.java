package com.cryptomessage.server.model.dto.chat;

public record ChatParticipantResponse(
        Long userId,
        String username,
        String publicKey
) {}
