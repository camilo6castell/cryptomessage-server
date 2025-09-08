package com.cryptomessage.server.model.dto.security.authentication;

import com.cryptomessage.server.model.dto.contact.ContactDTO;
import com.cryptomessage.server.model.dto.chat.ChatDTO;
import com.cryptomessage.server.services.RSAEncryptionWithPassphraseService;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.List;

public class AppUserDTO {

    private Long userId;
    private String token;
    private String username;
    private String publicKey;
    private String privateKey;
    private String createdAt;
    private List<ContactDTO> contacts;
    private List<ChatDTO> chats;

    public AppUserDTO() {
    }

    private AppUserDTO(Builder builder) {
        this.userId = builder.userId;
        this.token = builder.token;
        this.username = builder.username;
        this.publicKey = builder.publicKey;
        this.privateKey = builder.privateKey;
        this.createdAt = builder.createdAt;
        this.contacts = builder.contacts;
        this.chats = builder.chats;
    }

    // Getters y Setters

    public Long getUserId() {
        return userId;
    }

    public String getToken() {
        return token;
    }

    public String getUsername() {
        return username;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public List<ContactDTO> getContacts() {
        return contacts;
    }

    public List<ChatDTO> getChats() {
        return chats;
    }

    // Builder

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private Long userId;
        private String token;
        private String username;
        private String publicKey;
        private String privateKey;
        private String createdAt;
        private List<ContactDTO> contacts;
        private List<ChatDTO> chats;

        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public Builder token(String token) {
            this.token = token;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder publicKey(PublicKey publicKey) {
            this.publicKey = RSAEncryptionWithPassphraseService.publicKeyToString(publicKey);
            return this;
        }

        public Builder privateKey(PrivateKey privateKey) {
            this.privateKey = RSAEncryptionWithPassphraseService.privateKeyToString(privateKey);
            return this;
        }

        public Builder createdAt(String createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder contacts(List<ContactDTO> contacts) {
            this.contacts = contacts;
            return this;
        }

        public Builder chats(List<ChatDTO> chats) {
            this.chats = chats;
            return this;
        }

        public AppUserDTO build() {
            return new AppUserDTO(this);
        }
    }
}
