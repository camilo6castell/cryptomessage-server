package com.cryptomessage.server.model.dto.contact;

public final class AddContactRequest {

    private final Long contactId;

    public AddContactRequest(Long contactId) {
        this.contactId = contactId;
    }

    public Long getContactId() {
        return contactId;
    }
}

