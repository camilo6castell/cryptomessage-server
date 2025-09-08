package com.cryptomessage.server.model.persistance.chat;

import com.cryptomessage.server.model.persistance.message.Message;
import com.cryptomessage.server.model.persistance.user.AppUser;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "chats")
public class Chat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_id")
    private Long chatId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user1_id", nullable = false)
    private AppUser appUser1;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user2_id", nullable = false)
    private AppUser appUser2;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "chat", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Message> messages = new HashSet<>();

    // Constructor privado para forzar el uso del Builder
    private Chat(Builder builder) {
        this.chatId = builder.chatId;
        this.appUser1 = Objects.requireNonNull(builder.appUser1, "appUser1 cannot be null");
        this.appUser2 = Objects.requireNonNull(builder.appUser2, "appUser2 cannot be null");
        this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
        this.messages = builder.messages != null ? new HashSet<>(builder.messages) : new HashSet<>();
    }

    // Constructor sin argumentos para JPA
    protected Chat() {}

    // Getters
    public Long getChatId() {
        return chatId;
    }

    public AppUser getAppUser1() {
        return appUser1;
    }

    public AppUser getAppUser2() {
        return appUser2;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Set<Message> getMessages() {
        return Collections.unmodifiableSet(messages);
    }

    // Métodos específicos para manipular la colección de mensajes
    public void addMessage(Message message) {
        if (messages.add(message)) { // Añade solo si no está ya presente
            message.setChat(this);
        }
    }

    public void removeMessage(Message message) {
        if (messages.remove(message)) { // Elimina solo si estaba presente
            message.setChat(null);
        }
    }

    // Builder interno
    public static class Builder {
        private Long chatId;
        private AppUser appUser1;
        private AppUser appUser2;
        private LocalDateTime createdAt;
        private Set<Message> messages;

        public Builder withChatId(Long chatId) {
            this.chatId = chatId;
            return this;
        }

        public Builder withAppUser1(AppUser appUser1) {
            this.appUser1 = appUser1;
            return this;
        }

        public Builder withAppUser2(AppUser appUser2) {
            this.appUser2 = appUser2;
            return this;
        }

        public Builder withCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder withMessages(Set<Message> messages) {
            this.messages = messages != null ? new HashSet<>(messages) : new HashSet<>();
            return this;
        }

        public Chat build() {
            if (appUser1 == null || appUser2 == null) {
                throw new IllegalStateException("Both appUser1 and appUser2 are required");
            }
            return new Chat(this);
        }
    }

    // Método estático para obtener una instancia del builder
    public static Builder builder() {
        return new Builder();
    }

    // Implementación de equals, hashCode y toString
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Chat chat = (Chat) o;
        return Objects.equals(chatId, chat.chatId) &&
                Objects.equals(appUser1, chat.appUser1) &&
                Objects.equals(appUser2, chat.appUser2);
    }

    @Override
    public int hashCode() {
        return Objects.hash(chatId, appUser1, appUser2);
    }

    @Override
    public String toString() {
        return "Chat{" +
                "chatId=" + chatId +
                ", appUser1=" + appUser1 +
                ", appUser2=" + appUser2 +
                ", createdAt=" + createdAt +
                '}';
    }
}


//V0
//@Entity
//@Table(name = "chats")
//public class Chat {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    @Column(name = "chat_id")
//    private Long chatId;
//
//    @ManyToOne
//    @JoinColumn(name = "user1_id")
//    private AppUser appUser1;
//
//    @ManyToOne
//    @JoinColumn(name = "user2_id")
//    private AppUser appUser2;
//
//    @Column(name = "created_at", nullable = false)
//    private LocalDateTime createdAt;
//
//    @OneToMany(mappedBy = "chat", cascade = CascadeType.ALL, orphanRemoval = true)
//    private Set<Message> messages = new HashSet<>();
//
//    // Métodos específicos para manipular la colección de mensajes
//    public void addMessage(Message message) {
//        messages.add(message);
//        message.setChat(this);
//    }
//
//    public void removeMessage(Message message) {
//        messages.remove(message);
//        message.setChat(null);
//    }
//
//    // Duplicado abajo, remover y cambiar si da problemas
//
//    public Set<Message> getMessages() {
//        return Collections.unmodifiableSet(messages);
//    }
//
//    // Constructor privado para el Builder
//    private Chat(Builder builder) {
//        this.chatId = builder.chatId;
//        this.appUser1 = builder.appUser1;
//        this.appUser2 = builder.appUser2;
//        this.createdAt = builder.createdAt;
//        this.messages = builder.messages != null ? Collections.unmodifiableSet(builder.messages) : Collections.emptySet();
//    }
//
//    public Chat() {
//
//    }
//
//    // Getters
//    public Long getChatId() {
//        return chatId;
//    }
//
//    public AppUser getUser1() {
//        return appUser1;
//    }
//
//    public AppUser getUser2() {
//        return appUser2;
//    }
//
//    public LocalDateTime getCreatedAt() {
//        return createdAt;
//    }
//
//    // Builder interno
//    public static class Builder {
//        private Long chatId;
//        private AppUser appUser1;
//        private AppUser appUser2;
//        private LocalDateTime createdAt;
//        private Set<Message> messages;
//
//        public Builder withChatId(Long chatId) {
//            this.chatId = chatId;
//            return this;
//        }
//
//        public Builder withUser1(AppUser appUser1) {
//            this.appUser1 = appUser1;
//            return this;
//        }
//
//        public Builder withUser2(AppUser appUser2) {
//            this.appUser2 = appUser2;
//            return this;
//        }
//
//        public Builder withCreatedAt(LocalDateTime createdAt) {
//            this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
//            return this;
//        }
//
//        public Builder withMessages(Set<Message> messages) {
//            this.messages = messages;
//            return this;
//        }
//
//        public Chat build() {
//            if (appUser1 == null || appUser2 == null || createdAt == null) {
//                throw new IllegalStateException("Required fields are missing");
//            }
//            return new Chat(this);
//        }
//    }
//
//    // Método estático para obtener una instancia del builder
//    public static Builder builder() {
//        return new Builder();
//    }
//}
