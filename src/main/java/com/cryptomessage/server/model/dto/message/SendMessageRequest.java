package com.cryptomessage.server.model.dto.message;

import java.util.Map;

public record SendMessageRequest(Long chatId, Map<Long, String> encryptedContentByUser) {}