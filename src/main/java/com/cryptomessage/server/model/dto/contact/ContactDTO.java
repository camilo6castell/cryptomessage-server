package com.cryptomessage.server.model.dto.contact;

import com.cryptomessage.server.services.RSAEncryptionWithPassphraseService;

import java.security.PublicKey;
import java.util.Objects;

public class ContactDTO {
    private final Long contactId;
    private final String username;
    private final String publicKey;

    // Constructor privado para forzar el uso del Builder
    private ContactDTO(Builder builder) {
        this.contactId = builder.contactId;
        this.username = builder.username;
        this.publicKey = builder.publicKey;
    }

    // Getters
    public Long getContactId() {
        return contactId;
    }

    public String getUsername() {
        return username;
    }

    public String getPublicKey() {
        return publicKey;
    }

    // Builder

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long contactId;
        private String username;
        private String publicKey;

        public Builder withContactId(Long contactId) {
            this.contactId = contactId;
            return this;
        }

        public Builder withUsername(String username) {
            this.username = username;
            return this;
        }

        public Builder withPublicKey(PublicKey publicKey) {
            this.publicKey = RSAEncryptionWithPassphraseService.publicKeyToString(publicKey);
            return this;
        }

        public ContactDTO build() {
            return new ContactDTO(this);
        }
    }

    // Implementación de equals, hashCode y toString
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ContactDTO that = (ContactDTO) o;
        return Objects.equals(contactId, that.contactId) &&
                Objects.equals(username, that.username) &&
                Objects.equals(publicKey, that.publicKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(contactId, username, publicKey);
    }

    @Override
    public String toString() {
        return "ContactResponse{" +
                "contactId=" + contactId +
                ", username='" + username + '\'' +
                ", publicKey='" + publicKey + '\'' +
                '}';
    }
}
