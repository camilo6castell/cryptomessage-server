package com.cryptomessage.server.infrastructure.projection;

import com.cryptomessage.server.domain.chat.events.ChatAccepted;
import com.cryptomessage.server.domain.chat.events.ChatCreated;
import com.cryptomessage.server.domain.chat.events.ChatRead;
import com.cryptomessage.server.domain.chat.events.MessageSent;
import com.cryptomessage.server.domain.generic.DomainEvent;
import com.cryptomessage.server.model.entity.chat.Chat;
import com.cryptomessage.server.model.entity.contact.Contact;
import com.cryptomessage.server.model.entity.contact.ContactId;
import com.cryptomessage.server.model.entity.message.Message;
import com.cryptomessage.server.model.entity.user.AppUser;
import com.cryptomessage.server.repositories.ChatRepository;
import com.cryptomessage.server.repositories.ContactRepository;
import com.cryptomessage.server.repositories.MessageRepository;
import com.cryptomessage.server.repositories.UserRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Turns newly-appended Chat domain events into updates on the read-model tables
 * (`chats`, `messages`, `contacts` — the same JPA entities and repositories that
 * existed before this migration). This is the "Q" side of CQRS: MessageController
 * and ChatController's read endpoints query these tables directly and never touch
 * the event store or the aggregate.
 *
 * Runs in the SAME transaction as the event append (use cases call
 * ChatAggregateRepository#save and this projector back-to-back inside one
 * @Transactional method) rather than asynchronously. That trade-off is
 * deliberate: this app has no message queue or outbox today, and eventual
 * consistency between "I sent a message" and "the recipient's chat list shows
 * it" would be a user-visible regression in a chat app. If this ever needs to
 * scale past a single DB transaction's throughput, introducing an outbox +
 * async projection is the natural next step — but it's not needed yet, and
 * doing it prematurely would add real complexity for no current benefit.
 */
@Component
public class ChatProjector {

    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;
    private final ContactRepository contactRepository;
    private final UserRepository userRepository;

    public ChatProjector(
            ChatRepository chatRepository,
            MessageRepository messageRepository,
            ContactRepository contactRepository,
            UserRepository userRepository
    ) {
        this.chatRepository = chatRepository;
        this.messageRepository = messageRepository;
        this.contactRepository = contactRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void project(Long chatId, List<DomainEvent> events) {
        for (DomainEvent event : events) {
            if (event instanceof ChatCreated e) {
                projectChatCreated(chatId, e);
            } else if (event instanceof MessageSent e) {
                projectMessageSent(chatId, e);
            } else if (event instanceof ChatAccepted ignored) {
                projectChatAccepted(chatId);
            } else if (event instanceof ChatRead e) {
                projectChatRead(chatId, e);
            }
            // Unknown event types are ignored on purpose: a projector should never
            // fail hard just because a newer event type hasn't been wired up for
            // read-model purposes yet. It's still safely durable in the event store.
        }
    }

    private void projectChatCreated(Long chatId, ChatCreated event) {
        AppUser user1 = findUser(event.getUser1Id());
        AppUser user2 = findUser(event.getUser2Id());
        AppUser initiatedBy = findUser(event.getInitiatedByUserId());

        Chat chat = new Chat(chatId, user1, user2, initiatedBy);
        chatRepository.save(chat);
    }

    /**
     * Public on purpose: SendMessageUseCase calls this directly (rather than the
     * generic {@link #project}) because it needs the created Message row back —
     * its DB-assigned messageId and sentAt are part of MessageResponse and the
     * WebSocket push, exactly as they were before this migration.
     */
    public Message projectMessageSent(Long chatId, MessageSent event) {
        Chat chat = findChat(chatId);
        AppUser sender = findUser(event.getSenderId());

        Message message = new Message(chat, sender, event.getContentByUser());
        chat.addMessage(message);
        return messageRepository.save(message);
    }

    private void projectChatAccepted(Long chatId) {
        Chat chat = findChat(chatId);
        chat.accept();
        chatRepository.save(chat);

        maybeCreateContact(chat);
    }

    private void projectChatRead(Long chatId, ChatRead event) {
        Chat chat = findChat(chatId);
        AppUser reader = findUser(event.getReadByUserId());
        messageRepository.markAsReadByChatAndNotSender(chat, reader);
    }

    // Mirrors the contact-creation side effect the old ChatService.acceptChat()
    // used to perform inline. Kept idempotent (existsById check) since Contact
    // was deliberately left out of Event Sourcing — see the earlier design
    // discussion — so there's no event log to lean on for "did this already
    // happen" here.
    private void maybeCreateContact(Chat chat) {
        AppUser a = chat.getAppUser1();
        AppUser b = chat.getAppUser2();

        createContactIfAbsent(a, b);
        createContactIfAbsent(b, a);
    }

    private void createContactIfAbsent(AppUser owner, AppUser contactUser) {
        ContactId id = new ContactId(owner.getUserId(), contactUser.getUserId());
        if (!contactRepository.existsById(id)) {
            contactRepository.save(new Contact(owner, contactUser));
        }
    }

    private AppUser findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));
    }

    private Chat findChat(Long chatId) {
        return chatRepository.findById(chatId)
                .orElseThrow(() -> new NoSuchElementException("Chat projection not found: " + chatId));
    }
}
