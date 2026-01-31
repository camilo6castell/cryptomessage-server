package com.cryptomessage.server.model.dto.contact;

public final class RemoveContactRequest {

    private final Long contactId;

    public RemoveContactRequest(Long contactId) {
        this.contactId = contactId;
    }

    public Long getContactId() {
        return contactId;
    }
}

