package com.cryptomessage.server.model.dto.contact;

public class TransactionContactRequest {
    private Long appUserId;
    private Long contactId;

    public TransactionContactRequest(Long contactId, Long appUserId) {
        this.contactId = contactId;
        this.appUserId = appUserId;
    }

    public TransactionContactRequest() {
    }

    public Long getContactId() {
        return contactId;
    }

    public void setContactId(Long contactId) {
        this.contactId = contactId;
    }

    public Long getAppUserId() {
        return appUserId;
    }

    public void setAppUserId(Long appUserId) {
        this.appUserId = appUserId;
    }
}
