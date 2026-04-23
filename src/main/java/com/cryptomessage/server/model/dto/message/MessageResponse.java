package com.cryptomessage.server.model.dto.message;

import java.time.Instant;

public record MessageResponse(Long messageId, Long chatId, Long senderId, String encryptedContent, boolean read,
                              Instant sentAt) {

}
