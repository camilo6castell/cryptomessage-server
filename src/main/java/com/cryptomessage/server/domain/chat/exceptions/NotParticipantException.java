package com.cryptomessage.server.domain.chat.exceptions;

/** Maps to 400, same as the original IllegalArgumentException("User not part of this chat"). */
public class NotParticipantException extends ChatDomainException {
    public NotParticipantException(String message) {
        super(message);
    }
}
