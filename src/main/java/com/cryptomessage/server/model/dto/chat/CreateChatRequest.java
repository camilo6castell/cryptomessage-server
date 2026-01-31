package com.cryptomessage.server.model.dto.chat;

public class CreateChatRequest {

    private Long contactId;

    public CreateChatRequest() {
    }

    public CreateChatRequest(Long contactId) {
        this.contactId = contactId;
    }

    public Long getContactId() {
        return contactId;
    }

    public void setContactId(Long contactId) {
        this.contactId = contactId;
    }
}

