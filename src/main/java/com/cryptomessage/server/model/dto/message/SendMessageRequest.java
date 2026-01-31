package com.cryptomessage.server.model.dto.message;

import java.util.Map;

public final class SendMessageRequest {

    private final Long chatId;
    private final Map<Long, String> encryptedContentByUser;

    public SendMessageRequest(Long chatId, Map<Long, String> encryptedContentByUser) {
        this.chatId = chatId;
        this.encryptedContentByUser = encryptedContentByUser;
    }

    public Long getChatId() {
        return chatId;
    }
    public Map<Long, String> getEncryptedContentByUser() {
        return encryptedContentByUser;
    }
}
