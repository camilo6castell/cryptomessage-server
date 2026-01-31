package com.cryptomessage.server.model.dto.message;

import java.time.Instant;

public final class MessageResponse {

    private final Long messageId;
    private final Long chatId;
    private final Long senderId;
    private final String encryptedContent;
    private final boolean read;
    private final Instant sentAt;

    public MessageResponse(
            Long messageId,
            Long chatId,
            Long senderId,
            String encryptedContent,
            boolean read,
            Instant sentAt
    ) {
        this.messageId = messageId;
        this.chatId = chatId;
        this.senderId = senderId;
        this.encryptedContent = encryptedContent;
        this.read = read;
        this.sentAt = sentAt;
    }

    public Long getMessageId() {
        return messageId;
    }

    public Long getChatId() {
        return chatId;
    }

    public Long getSenderId() {
        return senderId;
    }

    public String getEncryptedContent() {
        return encryptedContent;
    }

    public boolean isRead() {
        return read;
    }

    public Instant getSentAt() {
        return sentAt;
    }
}
