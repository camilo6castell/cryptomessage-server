package com.cryptomessage.server.domain.chat;

import com.cryptomessage.server.domain.generic.Identity;

import java.util.UUID;

public final class ChatId extends Identity {
    private ChatId(String uuid) {
        super(uuid);
    }

    public static ChatId generate() {
        return new ChatId(UUID.randomUUID().toString());
    }

    public static ChatId of(String uuid) {
        return new ChatId(uuid);
    }
}
