package com.cryptomessage.server.domain.chat.exceptions;

/** Maps to 400, same as the original "Encrypted content must exist for both participants". */
public class InvalidEncryptedContentException extends ChatDomainException {
    public InvalidEncryptedContentException(String message) {
        super(message);
    }
}
