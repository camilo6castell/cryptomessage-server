package com.cryptomessage.server.domain.chat.exceptions;

/**
 * Defensive check inside the aggregate itself. In practice CreateChatUseCase
 * rejects a self-chat earlier (as a 409, matching the original ChatService
 * behavior) — this exception is the aggregate's own belt-and-suspenders
 * invariant, mirroring the original Chat JPA entity's redundant validation,
 * and should be unreachable in normal operation.
 */
public class SameParticipantException extends ChatDomainException {
    public SameParticipantException(String message) {
        super(message);
    }
}
