package com.cryptomessage.server.domain.chat.events;

import com.cryptomessage.server.domain.generic.DomainEvent;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class MessageSent extends DomainEvent {

    private final String messageId;
    private final Long senderId;
    // Ciphertext only, one blob per recipient — same guarantee the current
    // `content_by_user` JPA column already provides. Event Sourcing does not
    // introduce a new place where plaintext could leak.
    private final Map<Long, String> contentByUser;

    @JsonCreator
    public MessageSent(
            @JsonProperty("messageId") String messageId,
            @JsonProperty("senderId") Long senderId,
            @JsonProperty("contentByUser") Map<Long, String> contentByUser
    ) {
        super();
        this.messageId = messageId;
        this.senderId = senderId;
        this.contentByUser = Map.copyOf(contentByUser);
    }

    public String getMessageId() { return messageId; }
    public Long getSenderId() { return senderId; }
    public Map<Long, String> getContentByUser() { return contentByUser; }
}
