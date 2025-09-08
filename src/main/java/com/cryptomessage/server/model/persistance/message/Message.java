package com.cryptomessage.server.model.persistance.message;

import com.cryptomessage.server.model.persistance.chat.Chat;
import com.cryptomessage.server.model.persistance.user.AppUser;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

@Entity
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Long messageId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chat_id", nullable = false)
    private Chat chat;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private AppUser sender;

    @Column(name = "content_by_user", nullable = false, columnDefinition = "TEXT")
    @Convert(converter = ContentByUserConverter.class)
    private Map<Long, String> contentByUser;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    @Column(name = "is_read", nullable = false)
    private boolean isRead;

    // Constructor privado para forzar el uso del Builder
    private Message(Builder builder) {
        this.messageId = builder.messageId;
        this.chat = Objects.requireNonNull(builder.chat, "Chat cannot be null");
        this.sender = Objects.requireNonNull(builder.sender, "Sender cannot be null");
        this.contentByUser = Objects.requireNonNull(builder.contentByUser, "ContentByUser cannot be null");
        this.sentAt = builder.sentAt != null ? builder.sentAt : LocalDateTime.now();
        this.isRead = builder.isRead;
    }

    // Constructor sin argumentos para JPA
    protected Message() {}

    // Getters (sin setters para mantener la inmutabilidad)
    public Long getMessageId() {
        return messageId;
    }

    public Chat getChat() {
        return chat;
    }

    public AppUser getSender() {
        return sender;
    }

    public Map<Long, String> getContentByUser() {
        return contentByUser;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public boolean getIsRead() {
        return isRead;
    }

    // Setter para la relación con Chat
    public void setChat(Chat chat) {
        this.chat = chat;
    }

    public void setIsRead(boolean isRead) {
        this.isRead = isRead;
    }

    // Builder interno
    public static class Builder {
        private Long messageId;
        private Chat chat;
        private AppUser sender;
        private Map<Long, String> contentByUser;
        private LocalDateTime sentAt;
        private boolean isRead;

        public Builder withMessageId(Long messageId) {
            this.messageId = messageId;
            return this;
        }

        public Builder withChat(Chat chat) {
            this.chat = chat;
            return this;
        }

        public Builder withSender(AppUser sender) {
            this.sender = sender;
            return this;
        }

        public Builder withContentByUser(Map<Long, String> contentByUser) {
            this.contentByUser = contentByUser;
            return this;
        }

        public Builder withSentAt(LocalDateTime sentAt) {
            this.sentAt = sentAt;
            return this;
        }

        public Builder withIsRead(boolean isRead) {
            this.isRead = isRead;
            return this;
        }

        public Message build() {
            if (chat == null || sender == null || contentByUser == null) {
                throw new IllegalStateException("Chat, sender, and contentByUser are required fields");
            }
            return new Message(this);
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
        Message message = (Message) o;
        return Objects.equals(messageId, message.messageId) &&
                Objects.equals(chat, message.chat) &&
                Objects.equals(sender, message.sender) &&
                Objects.equals(contentByUser, message.contentByUser) &&
                Objects.equals(sentAt, message.sentAt) &&
                Objects.equals(isRead, message.isRead);
    }

    @Override
    public int hashCode() {
        return Objects.hash(messageId, chat, sender, contentByUser, sentAt, isRead);
    }

    @Override
    public String toString() {
        return "Message{" +
                "messageId=" + messageId +
                ", chat=" + chat +
                ", sender=" + sender +
                ", contentByUser=" + contentByUser +
                ", sentAt=" + sentAt +
                ", isRead=" + isRead +
                '}';
    }
}

// V0
//@Entity
//@Table(name = "messages")
//public class Message {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    @Column(name = "message_id")
//    private Long messageId;
//
//    @ManyToOne(fetch = FetchType.LAZY, optional = false)
//    @JoinColumn(name = "chat_id", nullable = false)
//    private Chat chat;
//
//    @ManyToOne(fetch = FetchType.LAZY, optional = false)
//    @JoinColumn(name = "sender_id", nullable = false)
//    private AppUser sender;
//
//    @Column(name = "content_by_user", nullable = false, columnDefinition = "TEXT")
//    @Convert(converter = ContentByUserConverter.class)
//    private Map<Long, String> contentByUser;
//
//    @Column(name = "sent_at", nullable = false)
//    private LocalDateTime sentAt;
//
//    // Constructor privado para forzar el uso del Builder
//    private Message(Builder builder) {
//        this.messageId = builder.messageId;
//        this.chat = Objects.requireNonNull(builder.chat, "Chat cannot be null");
//        this.sender = Objects.requireNonNull(builder.sender, "Sender cannot be null");
//        this.contentByUser = Objects.requireNonNull(builder.contentByUser, "ContentByUser cannot be null");
//        this.sentAt = builder.sentAt != null ? builder.sentAt : LocalDateTime.now();
//    }
//
//    // Constructor sin argumentos para JPA
//    protected Message() {}
//
//    // Getters (sin setters para mantener la inmutabilidad)
//    public Long getMessageId() {
//        return messageId;
//    }
//
//    public Chat getChat() {
//        return chat;
//    }
//
//    public AppUser getSender() {
//        return sender;
//    }
//
//    public Map<Long, String> getContentByUser() {
//        return contentByUser;
//    }
//
//    public LocalDateTime getSentAt() {
//        return sentAt;
//    }
//
//    // Setter para la relación con Chat
//    public void setChat(Chat chat) {
//        this.chat = chat;
//    }
//
//    // Builder interno
//    public static class Builder {
//        private Long messageId;
//        private Chat chat;
//        private AppUser sender;
//        private Map<Long, String> contentByUser;
//        private LocalDateTime sentAt;
//
//        public Builder withMessageId(Long messageId) {
//            this.messageId = messageId;
//            return this;
//        }
//
//        public Builder withChat(Chat chat) {
//            this.chat = chat;
//            return this;
//        }
//
//        public Builder withSender(AppUser sender) {
//            this.sender = sender;
//            return this;
//        }
//
//        public Builder withContentByUser(Map<Long, String> contentByUser) {
//            this.contentByUser = contentByUser;
//            return this;
//        }
//
//        public Builder withSentAt(LocalDateTime sentAt) {
//            this.sentAt = sentAt;
//            return this;
//        }
//
//        public Message build() {
//            if (chat == null || sender == null || contentByUser == null) {
//                throw new IllegalStateException("Chat, sender, and contentByUser are required fields");
//            }
//            return new Message(this);
//        }
//    }
//
//    // Método estático para obtener una instancia del builder
//    public static Builder builder() {
//        return new Builder();
//    }
//
//    // Implementación de equals, hashCode y toString
//    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true;
//        if (o == null || getClass() != o.getClass()) return false;
//        Message message = (Message) o;
//        return Objects.equals(messageId, message.messageId) &&
//                Objects.equals(chat, message.chat) &&
//                Objects.equals(sender, message.sender) &&
//                Objects.equals(contentByUser, message.contentByUser) &&
//                Objects.equals(sentAt, message.sentAt);
//    }
//
//    @Override
//    public int hashCode() {
//        return Objects.hash(messageId, chat, sender, contentByUser, sentAt);
//    }
//
//    @Override
//    public String toString() {
//        return "Message{" +
//                "messageId=" + messageId +
//                ", chat=" + chat +
//                ", sender=" + sender +
//                ", contentByUser=" + contentByUser +
//                ", sentAt=" + sentAt +
//                '}';
//    }
//}