package com.cryptomessage.server.domain.chat.exceptions;

/**
 * Base type for Chat aggregate invariant violations. Deliberately does NOT extend
 * any Spring/web exception type — the domain layer must not know how a violation
 * gets translated into an HTTP response. That mapping lives in
 * GlobalExceptionHandler, at the boundary, matching the exact status codes the
 * frontend already relies on today.
 */
public class ChatDomainException extends RuntimeException {
    public ChatDomainException(String message) {
        super(message);
    }
}
