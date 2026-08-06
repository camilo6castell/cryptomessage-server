package com.cryptomessage.server.domain.chat.exceptions;

/** Maps to 403, same as the original ForbiddenException("Chat not accepted yet"). */
public class ChatNotAcceptedException extends ChatDomainException {
    public ChatNotAcceptedException(String message) {
        super(message);
    }
}
