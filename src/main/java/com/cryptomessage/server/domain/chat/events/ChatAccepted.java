package com.cryptomessage.server.domain.chat.events;

import com.cryptomessage.server.domain.generic.DomainEvent;
import com.fasterxml.jackson.annotation.JsonCreator;

public class ChatAccepted extends DomainEvent {
    @JsonCreator
    public ChatAccepted() {
        super();
    }
}
