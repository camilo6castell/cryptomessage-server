package com.cryptomessage.server.model.entity.user;

import com.cryptomessage.server.model.entity.chat.Chat;
import com.cryptomessage.server.model.entity.contact.Contact;
import jakarta.persistence.*;

import java.security.PublicKey;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_users_username",
                        columnNames = "username"
                )
        }
)
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "username", nullable = false, updatable = false)
    private String username;

    @Column(name = "passphrase_hash", nullable = false)
    private String passphraseHash;

    @Column(name = "encrypted_private_key", nullable = false, columnDefinition = "TEXT")
    private String encryptedPrivateKey;

    @Convert(converter = PublicKeyConverter.class)
    @Column(name = "public_key", nullable = false, columnDefinition = "TEXT")
    private PublicKey publicKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ===== RELATIONS =====

    @OneToMany(
            mappedBy = "appUser1",
            fetch = FetchType.LAZY,
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private Set<Chat> initiatedChats = new HashSet<>();

    @OneToMany(
            mappedBy = "appUser2",
            fetch = FetchType.LAZY,
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private Set<Chat> receivedChats = new HashSet<>();

    @OneToMany(
            mappedBy = "appUser",
            fetch = FetchType.LAZY,
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private Set<Contact> contacts = new HashSet<>();

    // ===== JPA =====

    protected AppUser() {
        // JPA only
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // ===== BUILDER =====

    private AppUser(Builder builder) {
        this.username = builder.username;
        this.passphraseHash = builder.passphraseHash;
        this.encryptedPrivateKey = builder.encryptedPrivateKey;
        this.publicKey = builder.publicKey;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String username;
        private String passphraseHash;
        private String encryptedPrivateKey;
        private PublicKey publicKey;

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder passphraseHash(String passphraseHash) {
            this.passphraseHash = passphraseHash;
            return this;
        }

        public Builder encryptedPrivateKey(String encryptedPrivateKey) {
            this.encryptedPrivateKey = encryptedPrivateKey;
            return this;
        }

        public Builder publicKey(PublicKey publicKey) {
            this.publicKey = publicKey;
            return this;
        }

        public AppUser build() {
            if (username == null || passphraseHash == null || publicKey == null || encryptedPrivateKey == null) {
                throw new IllegalStateException(
                        "username, passphraseHash, encryptedPrivateKey and publicKey are required"
                );
            }
            return new AppUser(this);
        }
    }

    // ===== DOMAIN HELPERS =====

    /**
     * Devuelve todos los chats del usuario (iniciados y recibidos)
     */
    public Set<Chat> getAllChats() {
        Set<Chat> all = new HashSet<>(initiatedChats);
        all.addAll(receivedChats);
        return Collections.unmodifiableSet(all);
    }

    // ===== GETTERS =====

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getPassphraseHash() {
        return passphraseHash;
    }

    public String getEncryptedPrivateKey() {
        return encryptedPrivateKey;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Set<Contact> getContacts() {
        return Collections.unmodifiableSet(contacts);
    }

    public Set<Chat> getInitiatedChats() {
        return Collections.unmodifiableSet(initiatedChats);
    }

    public Set<Chat> getReceivedChats() {
        return Collections.unmodifiableSet(receivedChats);
    }
}
