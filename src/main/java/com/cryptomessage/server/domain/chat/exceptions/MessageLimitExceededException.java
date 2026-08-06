package com.cryptomessage.server.domain.chat.exceptions;

/** Maps to 403, same as the original "Only one message allowed until accepted". */
public class MessageLimitExceededException extends ChatDomainException {
    public MessageLimitExceededException(String message) {
        super(message);
    }
}
