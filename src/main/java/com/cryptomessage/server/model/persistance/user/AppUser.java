package com.cryptomessage.server.model.persistance.user;

import com.cryptomessage.server.model.dto.chat.ChatDTO;
import com.cryptomessage.server.model.dto.contact.ContactDTO;
import com.cryptomessage.server.model.dto.message.MessageDTO;
import com.cryptomessage.server.model.persistance.chat.Chat;
import com.cryptomessage.server.model.persistance.contact.Contact;
import jakarta.persistence.*;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "users")
public class AppUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private final Long userId;

    @Column(name = "username", nullable = false, unique = true)
    private final String username;

    @Column(name = "passphrase_hash", nullable = false)
    private final String passphraseHash;

    @Column(name = "encrypted_private_key", columnDefinition = "TEXT")
    private final String encryptedPrivateKey;

//    @Column(name = "encrypted_private_key", columnDefinition = "TEXT")
//    @Convert(converter = PrivateKeyConverter.class)
//    private final PrivateKey encryptedPrivateKey;

//    @Column(name = "public_key", nullable = false, columnDefinition = "TEXT")
//    private final String publicKey;

    @Column(name = "public_key", nullable = false, columnDefinition = "TEXT")
    @Convert(converter = PublicKeyConverter.class)
    private final PublicKey publicKey;

    @Column(name = "created_at", nullable = false)
    private final LocalDateTime createdAt;

    @OneToMany(mappedBy = "appUser1", cascade = CascadeType.ALL, orphanRemoval = true)
    private final Set<Chat> initiatedChats;

    @OneToMany(mappedBy = "appUser2", cascade = CascadeType.ALL, orphanRemoval = true)
    private final Set<Chat> receivedChats;

    @OneToMany(mappedBy = "appUser", cascade = CascadeType.ALL, orphanRemoval = true)
    private final Set<Contact> contacts;


    protected AppUser() {
        // Constructor protegido para JPA
        this.userId = null;
        this.username = null;
        this.passphraseHash = null;
        this.encryptedPrivateKey = null;
        this.publicKey = null;
        this.createdAt = null;
        this.initiatedChats = new HashSet<>();
        this.receivedChats = new HashSet<>();
        this.contacts = new HashSet<>();
    }

    private AppUser(Builder builder) {
        this.userId = builder.userId;
        this.username = builder.username;
        this.passphraseHash = builder.passphraseHash;
        this.encryptedPrivateKey = builder.encryptedPrivateKey;
        this.publicKey = builder.publicKey;
        this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
        this.contacts = builder.contacts != null ? builder.contacts : new HashSet<>();
        this.initiatedChats = builder.initiatedChats != null ? builder.initiatedChats : new HashSet<>();
        this.receivedChats = builder.receivedChats != null ? builder.receivedChats : new HashSet<>();
    }

    // Método para consolidar los chats
    public Set<Chat> getAllChats() {
        Set<Chat> allChats = new HashSet<>(initiatedChats);
        allChats.addAll(receivedChats);
        return Collections.unmodifiableSet(allChats);
    }

    // Método para obtener contactos como ContactResponse
    public List<ContactDTO> getContactsAsDTOs() {
        return this.contacts.stream()
                .map(contact -> new ContactDTO.Builder()
                        .withContactId(contact.getContact().getUserId())
                        .withUsername(contact.getContact().getUsername())
                        .withPublicKey(contact.getContact().getPublicKey())
                        .build())
                .collect(Collectors.toList());
    }

    // Método para obtener chats como ChatResponse
    public List<ChatDTO> getChatsAsDTOs(Long appUserId) {
        return this.getAllChats().stream()
                .map(chat -> new ChatDTO.Builder()
                        .withChatId(chat.getChatId())
                        .withParticipants(List.of(
                                new ContactDTO.Builder()
                                        .withContactId(chat.getAppUser1().getUserId())
                                        .withUsername(chat.getAppUser1().getUsername())
                                        .withPublicKey(chat.getAppUser1().getPublicKey())
                                        .build(),
                                new ContactDTO.Builder()
                                        .withContactId(chat.getAppUser2().getUserId())
                                        .withUsername(chat.getAppUser2().getUsername())
                                        .withPublicKey(chat.getAppUser2().getPublicKey())
                                        .build()
                        ))
                        .withMessages(chat.getMessages().stream()
                                .map(message -> new MessageDTO.Builder()
                                        .withChatId(message.getChat().getChatId())
                                        .withMessageId(message.getMessageId())
                                        .withSenderId(message.getSender().getUserId())
                                        .withContent(
                                                message.getSender().getUserId().equals(
                                                        appUserId) ?
                                                        message.getContentByUser().get(1L)
                                                        :
                                                        message.getContentByUser().get(2L))
                                        .withIsRead(message.getIsRead())
                                        .withSentAt(message.getSentAt().toString())
                                        .build())
                                .collect(Collectors.toList()))
                        .build())
                .collect(Collectors.toList());
    }

    // Builder con validaciones en el método build
    public static class Builder {
        private Long userId;
        private String username;
        private String passphraseHash;
        private String encryptedPrivateKey;
        private PublicKey publicKey;
        private LocalDateTime createdAt;
        private Set<Contact> contacts;
        private Set<Chat> initiatedChats;
        private Set<Chat> receivedChats;

        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

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

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder contacts(Set<Contact> contacts) {
            this.contacts = contacts;
            return this;
        }

        public Builder initiatedChats(Set<Chat> initiatedChats) {
            this.initiatedChats = initiatedChats;
            return this;
        }

        public Builder receivedChats(Set<Chat> receivedChats) {
            this.receivedChats = receivedChats;
            return this;
        }

        public AppUser build() {
            if (username == null || passphraseHash == null || publicKey == null) {
                throw new IllegalStateException("Fields 'username', 'passphraseHash', and 'publicKey' are required");
            }
            return new AppUser(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getters (sin setters para mantener la inmutabilidad)

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


//@Entity
//@Table(name = "users")
//public class AppUser {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    @Column(name = "user_id")
//    private Long userId;
//
//    @Column(name = "username", nullable = false, unique = true)
//    private String username;
//
//    @Column(name = "passphrase_hash", nullable = false)
//    private String passphraseHash;
//
//    @Column(name = "encrypted_private_key", columnDefinition = "TEXT")
//    private String encryptedPrivateKey;
//
//    @Column(name = "public_key", nullable = false, columnDefinition = "TEXT")
//    private String publicKey;
//
//    @Column(name = "created_at", nullable = false)
//    private LocalDateTime createdAt;
//
//    @OneToMany(mappedBy = "appUser1", cascade = CascadeType.ALL, orphanRemoval = true)
//    private Set<Chat> initiatedChats = new HashSet<>();
//
//    @OneToMany(mappedBy = "appUser2", cascade = CascadeType.ALL, orphanRemoval = true)
//    private Set<Chat> receivedChats = new HashSet<>();
//
//    @OneToMany(mappedBy = "appUser", cascade = CascadeType.ALL, orphanRemoval = true)
//    private Set<Contact> contacts = new HashSet<>();
//
//    protected AppUser() {
//    }
//
//    private AppUser(Builder builder) {
//        this.userId = builder.userId;
//        this.username = builder.username;
//        this.passphraseHash = builder.passphraseHash;
//        this.encryptedPrivateKey = builder.encryptedPrivateKey;
//        this.publicKey = builder.publicKey;
//        this.createdAt = builder.createdAt;
//        this.contacts = builder.contacts;
//        this.initiatedChats = builder.initiatedChats;
//        this.receivedChats = builder.receivedChats;
//    }
//
//    // Método para consolidar los chats
//    public Set<Chat> getAllChats() {
//        Set<Chat> allChats = new HashSet<>(initiatedChats);
//        allChats.addAll(receivedChats);
//        return Collections.unmodifiableSet(allChats);
//    }
//
//    // Método para obtener contactos como ContactResponse
//    public List<ContactDTO> getContactsAsDTOs() {
//        return this.getContacts().stream()
//                .map(contact -> new ContactDTO.Builder()
//                        .withContactId(contact.getContact().getUserId())
//                        .withUsername(contact.getContact().getUsername())
//                        .withPublicKey(contact.getContact().getPublicKey())
//                        .build())
//                .collect(Collectors.toList());
//    }
//
//    // Método para obtener chats como ChatResponse
//    public List<ChatDTO> getChatsAsDTOs() {
//        return this.getAllChats().stream()
//                .map(chat -> new ChatDTO.Builder()
//                        .withChatId(chat.getChatId())
//                        .withParticipants(List.of(
//                                new ContactDTO.Builder()
//                                        .withContactId(chat.getUser1().getUserId())
//                                        .withUsername(chat.getUser1().getUsername())
//                                        .withPublicKey(chat.getUser1().getPublicKey())
//                                        .build(),
//                                new ContactDTO.Builder()
//                                        .withContactId(chat.getUser2().getUserId())
//                                        .withUsername(chat.getUser2().getUsername())
//                                        .withPublicKey(chat.getUser2().getPublicKey())
//                                        .build()
//                        ))
//                        .withMessages(chat.getMessages().stream()
//                                .map(message -> new MessageDTO.Builder()
//                                        .withChatId(message.getChat().getChatId())
//                                        .withMessageId(message.getMessageId())
//                                        .withSenderId(message.getSender().getUserId())
//                                        .withContent(message.getContent())
//                                        .withSentAt(message.getSentAt().toString())
//                                        .build())
//                                .collect(Collectors.toList()))
//                        .build())
//                .collect(Collectors.toList());
//    }
//
//
//    public static class Builder {
//        private Long userId;
//        private String username;
//        private String passphraseHash;
//        private String encryptedPrivateKey;
//        private String publicKey;
//        private LocalDateTime createdAt;
//        private Set<Contact> contacts;
//        private Set<Chat> initiatedChats;
//        private Set<Chat> receivedChats;
//
//        public Builder userId(Long userId) {
//            this.userId = userId;
//            return this;
//        }
//
//        public Builder username(String username) {
//            this.username = username;
//            return this;
//        }
//
//        public Builder passphraseHash(String passphraseHash) {
//            this.passphraseHash = passphraseHash;
//            return this;
//        }
//
//        public Builder encryptedPrivateKey(String encryptedPrivateKey) {
//            this.encryptedPrivateKey = encryptedPrivateKey;
//            return this;
//        }
//
//        public Builder publicKey(String publicKey) {
//            this.publicKey = publicKey;
//            return this;
//        }
//
//        public Builder createdAt(LocalDateTime createdAt) {
//            this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
//            return this;
//        }
//
//        public Builder contacts(Set<Contact> contacts) {
//            this.contacts = contacts;
//            return this;
//        }
//
//        public Builder initiatedChats(Set<Chat> initiatedChats) {
//            this.initiatedChats = initiatedChats;
//            return this;
//        }
//
//        public Builder receivedChats(Set<Chat> receivedChats) {
//            this.receivedChats = receivedChats;
//            return this;
//        }
//
//        public AppUser build() {
//            if (username == null || passphraseHash == null || publicKey == null || createdAt == null) {
//                throw new IllegalStateException("Required fields are missing");
//            }
//            return new AppUser(this);
//        }
//    }
//
//    public static Builder builder() {
//        return new Builder();
//    }
//
//    // Getters (no setters to keep immutability)
//
//    public Long getUserId() {
//        return userId;
//    }
//
//    public String getUsername() {
//        return username;
//    }
//
//    public String getPassphraseHash() {
//        return passphraseHash;
//    }
//
//    public String getEncryptedPrivateKey() {
//        return encryptedPrivateKey;
//    }
//
//    public String getPublicKey() {
//        return publicKey;
//    }
//
//    public LocalDateTime getCreatedAt() {
//        return createdAt;
//    }
//
//    public Set<Contact> getContacts() {
//        return contacts;
//    }
//
//    public Set<Chat> getinitiatedChats() {
//        return initiatedChats;
//    }
//
//    public Set<Chat> getreceivedChats() {
//        return receivedChats;
//    }
//}
