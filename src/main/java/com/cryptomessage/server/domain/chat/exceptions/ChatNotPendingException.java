package com.cryptomessage.server.domain.chat.exceptions;

/** Maps to 409, same as the original ConflictException("Chat is not pending"). */
public class ChatNotPendingException extends ChatDomainException {
    public ChatNotPendingException(String message) {
        super(message);
    }
}
