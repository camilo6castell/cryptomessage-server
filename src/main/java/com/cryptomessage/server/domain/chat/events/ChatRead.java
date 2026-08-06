package com.cryptomessage.server.domain.chat.events;

import com.cryptomessage.server.domain.generic.DomainEvent;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Raised when a participant marks a chat's messages as read. */
public class ChatRead extends DomainEvent {

    private final Long readByUserId;

    @JsonCreator
    public ChatRead(@JsonProperty("readByUserId") Long readByUserId) {
        super();
        this.readByUserId = readByUserId;
    }

    public Long getReadByUserId() { return readByUserId; }
}
