package com.cryptomessage.server.domain.chat.exceptions;

/** Maps to 403, same as the original "Initiator cannot accept their own chat". */
public class SelfAcceptanceException extends ChatDomainException {
    public SelfAcceptanceException(String message) {
        super(message);
    }
}
