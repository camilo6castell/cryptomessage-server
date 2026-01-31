package com.cryptomessage.server.model.entity.message;

import com.cryptomessage.server.model.entity.chat.Chat;
import com.cryptomessage.server.model.entity.user.AppUser;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

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

    @Convert(converter = ContentByUserConverter.class)
    @Column(name = "content_by_user", nullable = false, columnDefinition = "TEXT")
    private Map<Long, String> contentByUser;

    @Column(name = "sent_at", nullable = false, updatable = false)
    private LocalDateTime sentAt;

    @Column(name = "is_read", nullable = false)
    private boolean isRead;

    protected Message() {
        // Constructor protegido para JPA
    }

    public Message(
            Chat chat,
            AppUser sender,
            Map<Long, String> encryptedContentByUser
    ) {
        if (chat == null || sender == null || encryptedContentByUser == null) {
            throw new IllegalArgumentException("Chat, sender and content are required");
        }

        chat.assertUserIsParticipant(sender.getUserId());

        // 🔒 Validación fuerte: chat 1–1
        Set<Long> participantIds = chat.getParticipantIds();
        if (participantIds.size() != 2) {
            throw new IllegalStateException("Only 1–1 chats are supported");
        }

        if (!encryptedContentByUser.keySet().equals(participantIds)) {
            throw new IllegalArgumentException(
                    "Encrypted content must exist for both chat participants"
            );
        }

        this.chat = chat;
        this.sender = sender;
        this.contentByUser = Map.copyOf(encryptedContentByUser);
        this.isRead = false;
    }

    @PrePersist
    protected void onCreate() {
        this.sentAt = LocalDateTime.now();
    }

    // ===== Domain methods =====

    public void markAsRead() {
        this.isRead = true;
    }

    /**
     * Returns the encrypted content intended for the given user.
     * Throws if the user is not a participant of the chat.
     */
    public String getContentForUser(Long userId) {
        String content = contentByUser.get(userId);
        if (content == null) {
            throw new IllegalArgumentException("User has no access to this message");
        }
        return content;
    }

    /**
     * Internal access only. Avoid using this outside the domain.
     */
    Map<Long, String> getContentByUser() {
        return contentByUser;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public boolean isRead() {
        return isRead;
    }

    // ===== Getters =====

    public Long getMessageId() {
        return messageId;
    }

    public Chat getChat() {
        return chat;
    }

    public AppUser getSender() {
        return sender;
    }

    // ===== Infrastructure =====
    // Setter required by JPA to maintain bidirectional association.
    // Should only be called from Chat.addMessage()
    public void setChat(Chat chat) {
        this.chat = chat;
    }
}
