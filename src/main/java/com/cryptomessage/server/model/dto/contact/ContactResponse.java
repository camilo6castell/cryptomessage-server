package com.cryptomessage.server.model.dto.contact;

/**
 * DTO de salida para representar un contacto.
 * Representa al "otro usuario" dentro de una relación Contact.
 */
public final class ContactResponse {

    private final Long contactId;
    private final String username;
    private final String publicKey;

    public ContactResponse(Long contactId, String username, String publicKey) {
        this.contactId = contactId;
        this.username = username;
        this.publicKey = publicKey;
    }

    public Long getContactId() {
        return contactId;
    }

    public String getUsername() {
        return username;
    }

    public String getPublicKey() {
        return publicKey;
    }
}

