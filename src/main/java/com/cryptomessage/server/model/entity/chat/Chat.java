package com.cryptomessage.server.model.entity.chat;

import com.cryptomessage.server.model.entity.message.Message;
import com.cryptomessage.server.model.entity.user.AppUser;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
        name = "chats",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_chats_users",
                        columnNames = {"user1_id", "user2_id"}
                )
        }
)
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

    @Enumerated(EnumType.STRING)
    private ChatStatus status;

    @ManyToOne
    private AppUser initiatedBy;

    @OneToMany(
            mappedBy = "chat",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private final Set<Message> messages = new HashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;


    protected Chat() {
        // JPA
    }

    public Chat(AppUser a, AppUser b, AppUser initiatedBy) {
        if (a == null || b == null) {
            throw new IllegalArgumentException("Users cannot be null");
        }
        if (a.getUserId().equals(b.getUserId())) {
            throw new IllegalArgumentException("Cannot create chat with the same user");
        }

        if (a.getUserId() < b.getUserId()) {
            this.appUser1 = a;
            this.appUser2 = b;
        } else {
            this.appUser1 = b;
            this.appUser2 = a;
        }

        this.status = ChatStatus.PENDING;
        this.initiatedBy = initiatedBy;
    }


    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // ===== Domain methods =====

    /**
     * Aggregate root method.
     * Ensures bidirectional consistency and domain invariants.
     */
    public void addMessage(Message message) {
        if (message == null) {
            throw new IllegalArgumentException("Message cannot be null");
        }

        assertUserIsParticipant(message.getSender().getUserId());

        messages.add(message);
        message.setChat(this);
    }

    public void removeMessage(Message message) {
        if (message == null) return;

        messages.remove(message);
        message.setChat(null);
    }

    /**
     * Returns the other participant in a 1–1 chat.
     */
    public AppUser getOtherParticipant(Long userId) {
        if (appUser1.getUserId().equals(userId)) return appUser2;
        if (appUser2.getUserId().equals(userId)) return appUser1;
        throw new IllegalArgumentException("User is not part of this chat");
    }

    /**
     * Explicit helper for Message validation.
     */
    public Set<Long> getParticipantIds() {
        return Set.of(appUser1.getUserId(), appUser2.getUserId());
    }

    // ===== Getters =====

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

    public ChatStatus getStatus() {
        return status;
    }

    public AppUser getInitiatedBy() {
        return initiatedBy;
    }

    // ===== Setters =====

    public void accept() {
        if (this.status != ChatStatus.PENDING) {
            throw new IllegalStateException("Chat cannot be accepted");
        }
        this.status = ChatStatus.ACCEPTED;
    }

    public void block() {
        this.status = ChatStatus.BLOCKED;
    }

    // ===== Helpers =====

    public void assertUserIsParticipant(Long userId) {
        if (!appUser1.getUserId().equals(userId)
                && !appUser2.getUserId().equals(userId)) {
            throw new IllegalArgumentException("User not part of this chat");
        }
    }

    public boolean hasMessageFrom(AppUser user) {
        return messages.stream()
                .anyMatch(m -> m.getSender().equals(user));
    }
}
