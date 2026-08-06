package com.cryptomessage.server.domain.chat;

import com.cryptomessage.server.domain.generic.Identity;

import java.util.UUID;

/**
 * NOTE: the current read-model Message entity still uses a DB-assigned Long id
 * (GenerationType.IDENTITY) for backward compatibility with the existing API
 * response shape (MessageResponse#messageId is a Long, consumed by the frontend
 * as-is). This UUID is the *domain* identity used inside the event stream only —
 * the projector maps a fresh JPA row to it, and the Long id the frontend sees keeps
 * coming from that row, exactly as it does today.
 */
public final class MessageId extends Identity {
    private MessageId(String uuid) {
        super(uuid);
    }

    public static MessageId generate() {
        return new MessageId(UUID.randomUUID().toString());
    }

    public static MessageId of(String uuid) {
        return new MessageId(uuid);
    }
}
