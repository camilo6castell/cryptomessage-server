package com.cryptomessage.server.infrastructure.eventstore;

public class EventSerializationException extends RuntimeException {
    public EventSerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
