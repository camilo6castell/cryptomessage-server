package com.cryptomessage.server.domain.chat.events;

import com.cryptomessage.server.domain.generic.DomainEvent;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ChatCreated extends DomainEvent {

    private final Long user1Id; // lower userId, mirrors the ordering convention the JPA entity already used
    private final Long user2Id;
    private final Long initiatedByUserId;

    @JsonCreator
    public ChatCreated(
            @JsonProperty("user1Id") Long user1Id,
            @JsonProperty("user2Id") Long user2Id,
            @JsonProperty("initiatedByUserId") Long initiatedByUserId
    ) {
        super();
        this.user1Id = user1Id;
        this.user2Id = user2Id;
        this.initiatedByUserId = initiatedByUserId;
    }

    public Long getUser1Id() { return user1Id; }
    public Long getUser2Id() { return user2Id; }
    public Long getInitiatedByUserId() { return initiatedByUserId; }
}
