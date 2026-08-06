package com.cryptomessage.server.domain.chat;

import com.cryptomessage.server.domain.chat.events.ChatAccepted;
import com.cryptomessage.server.domain.chat.events.ChatCreated;
import com.cryptomessage.server.domain.chat.events.ChatRead;
import com.cryptomessage.server.domain.chat.events.MessageSent;
import com.cryptomessage.server.domain.chat.exceptions.*;
import com.cryptomessage.server.domain.generic.AggregateRoot;
import com.cryptomessage.server.domain.generic.DomainEvent;
import com.cryptomessage.server.domain.shared.UserId;
import com.cryptomessage.server.model.entity.chat.ChatStatus;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Chat aggregate root — the write-side, event-sourced source of truth for chat
 * invariants (who can message whom, and when).
 *
 * NOTE on reusing ChatStatus: this enum still lives in
 * {@code model.entity.chat.ChatStatus} (the read-model package) instead of a new
 * domain-owned type. A "purist" hexagonal layout would duplicate it here and map
 * between the two at the boundary. That duplication buys nothing for a
 * two-value enum and only adds a mapping step that could itself drift out of
 * sync — so it stays shared. If ChatStatus ever grows real domain behavior
 * beyond a label, split it then.
 *
 * NOTE on write-side state: this aggregate deliberately does NOT hold the full
 * message history in memory. It only tracks whether a message has already been
 * sent while the chat is PENDING — the one fact its own invariants need. The
 * queryable message history lives entirely in the read-model projection
 * (see infrastructure.projection.ChatProjector), which is what
 * MessageController actually reads from. This keeps reconstructing a Chat cheap
 * regardless of how many messages it has accumulated.
 */
public class Chat extends AggregateRoot<ChatId> {

    private UserId user1Id;
    private UserId user2Id;
    private UserId initiatedBy;
    private ChatStatus status;
    private boolean hasPendingMessage;

    private Chat(ChatId chatId) {
        super(chatId);
        subscribe(new ChatBehavior(this));
    }

    /**
     * NOTE on {@code chatId}: a "purist" aggregate would mint its own id internally
     * (ChatId.generate() originally did this with a random UUID). It doesn't here,
     * on purpose — the frontend's ChatResponse#chatId and every path variable
     * ({@code /api/v1/chats/{chatId}}) are typed as Long, and changing that is
     * exactly the kind of frontend-visible break this whole migration is meant to
     * avoid. Minting a Long id requires a database round-trip (see
     * infrastructure.eventstore.ChatIdGenerator), which is an I/O concern that
     * doesn't belong inside a pure domain factory method — so the use case asks
     * the generator for an id first, then passes it in here.
     */
    public static Chat create(ChatId chatId, UserId requesterId, UserId otherUserId, UserId initiatedBy) {
        if (requesterId.equals(otherUserId)) {
            // See the class-level note on SameParticipantException — CreateChatUseCase
            // rejects this earlier as a 409, so this branch is a defensive backstop.
            throw new SameParticipantException("Cannot create a chat with the same user twice");
        }

        boolean requesterIsLower = requesterId.value() < otherUserId.value();
        UserId first = requesterIsLower ? requesterId : otherUserId;
        UserId second = requesterIsLower ? otherUserId : requesterId;

        Chat chat = new Chat(chatId);
        chat.appendEvent(new ChatCreated(first.value(), second.value(), initiatedBy.value()));
        return chat;
    }

    /** Event sourcing reconstruction — replays only Chat's own stream, cheap by design. */
    public static Chat from(String chatId, List<DomainEvent> events) {
        if (events.isEmpty()) {
            throw new ChatDomainException("No events found for chat " + chatId);
        }
        Chat chat = new Chat(ChatId.of(chatId));
        events.forEach(chat::applyEvent);
        return chat;
    }

    public MessageId sendMessage(UserId senderId, Map<UserId, String> encryptedContentByUser) {
        assertParticipant(senderId);

        if (status == ChatStatus.PENDING) {
            if (!senderId.equals(initiatedBy)) {
                throw new ChatNotAcceptedException("Chat not accepted yet");
            }
            if (hasPendingMessage) {
                throw new MessageLimitExceededException("Only one message allowed until the chat is accepted");
            }
        }

        Set<UserId> participants = Set.of(user1Id, user2Id);
        if (!encryptedContentByUser.keySet().equals(participants)) {
            throw new InvalidEncryptedContentException("Encrypted content must exist for both participants");
        }

        MessageId messageId = MessageId.generate();
        Map<Long, String> rawContent = encryptedContentByUser.entrySet().stream()
                .collect(Collectors.toMap(entry -> entry.getKey().value(), Map.Entry::getValue));

        appendEvent(new MessageSent(messageId.value(), senderId.value(), rawContent));
        return messageId;
    }

    public void accept(UserId acceptingUserId) {
        assertParticipant(acceptingUserId);

        if (status != ChatStatus.PENDING) {
            throw new ChatNotPendingException("Chat is not pending");
        }
        if (acceptingUserId.equals(initiatedBy)) {
            throw new SelfAcceptanceException("Initiator cannot accept their own chat");
        }

        appendEvent(new ChatAccepted());
    }

    public void markAsRead(UserId readerId) {
        assertParticipant(readerId);
        appendEvent(new ChatRead(readerId.value()));
    }

    public UserId getOtherParticipant(UserId userId) {
        assertParticipant(userId);
        return userId.equals(user1Id) ? user2Id : user1Id;
    }

    private void assertParticipant(UserId userId) {
        if (!userId.equals(user1Id) && !userId.equals(user2Id)) {
            throw new NotParticipantException("User " + userId.value() + " is not part of this chat");
        }
    }

    // package-private mutators used only by ChatBehavior
    void setUser1Id(UserId v)     { this.user1Id = v; }
    void setUser2Id(UserId v)     { this.user2Id = v; }
    void setInitiatedBy(UserId v) { this.initiatedBy = v; }
    void setStatus(ChatStatus v)  { this.status = v; }
    void markPendingMessageSent() { this.hasPendingMessage = true; }

    public UserId getUser1Id()     { return user1Id; }
    public UserId getUser2Id()     { return user2Id; }
    public UserId getInitiatedBy() { return initiatedBy; }
    public ChatStatus getStatus()  { return status; }
}
