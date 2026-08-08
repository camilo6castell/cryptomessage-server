package com.cryptomessage.server.services;

import com.cryptomessage.server.domain.chat.events.MessageSent;
import com.cryptomessage.server.infrastructure.eventstore.StoredEventRepository;
import com.cryptomessage.server.model.entity.chat.Chat;
import com.cryptomessage.server.model.entity.chat.ChatStatus;
import com.cryptomessage.server.repositories.ChatRepository;
import com.cryptomessage.server.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import com.cryptomessage.server.repositories.MessageRepository;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class Scheduler {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ChatRepository chatRepository;
    private final StoredEventRepository storedEventRepository;

    public Scheduler(
            MessageRepository messageRepository,
            UserRepository userRepository,
            ChatRepository chatRepository,
            StoredEventRepository storedEventRepository
    ) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.chatRepository = chatRepository;
        this.storedEventRepository = storedEventRepository;
    }

    // 🧹 1. mensajes (15 días)
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void deleteOldMessages() {
        LocalDateTime limit = LocalDateTime.now().minusDays(15);

        // A chat can have at most one message while PENDING — see
        // Chat#sendMessage's own invariant. If that message is about to be
        // purged below, the chat would be left stuck: still PENDING, but
        // with no trace a message was ever sent, silently resetting
        // hasPendingMessage on the aggregate's next replay. Treat these
        // exactly like an abandoned chat and delete them now — read-model
        // row (cascades to its message via orphanRemoval) and full event
        // stream — instead of leaving that inconsistent window open until
        // deleteEmptyChats catches it, up to 15 days later. See
        // apuntes/05-decisiones-de-diseno/03-chats-pendientes-abandonados.md.
        List<Chat> stuckPendingChats = chatRepository.findChatsByStatusWithMessagesOlderThan(
                ChatStatus.PENDING, limit
        );
        List<String> stuckPendingChatIds = stuckPendingChats.stream()
                .map(chat -> String.valueOf(chat.getChatId()))
                .toList();

        chatRepository.deleteAll(stuckPendingChats); // entity-based delete → cascade removes the message(s) too
        stuckPendingChatIds.forEach(storedEventRepository::deleteByAggregateId);

        // Normal path: aging messages in ACCEPTED chats. The set of PENDING
        // chats reaching this point should now be empty, since any that
        // qualified were already handled above.
        messageRepository.deleteOlderThan(limit);

        // NOTE: Event Sourcing's "never delete anything" default would silently
        // undermine this app's privacy-by-design retention policy — old
        // ciphertext would keep living in the event store forever even after
        // supposedly being deleted. So the retention job purges the
        // corresponding MessageSent events too. ChatCreated/ChatAccepted for
        // the same chat are deliberately left alone: the chat itself isn't
        // being deleted here, only its aging messages, matching the original
        // MessageRepository#deleteOlderThan behavior exactly.
        storedEventRepository.deleteByEventTypeAndOccurredOnBefore(
                MessageSent.class.getName(), limit
        );
    }

    // 💬 2. chats vacíos (30 días)
    @Scheduled(cron = "0 15 3 * * *")
    @Transactional
    public void deleteEmptyChats() {
        LocalDateTime limit = LocalDateTime.now().minusDays(30);

        // Read the ids before the delete query removes the read-model rows,
        // so the same set can be used to purge their event streams too.
        var emptyChatIds = chatRepository.findEmptyChatsOlderThan(limit)
                .stream()
                .map(chat -> String.valueOf(chat.getChatId()))
                .toList();

        chatRepository.deleteEmptyChatsOlderThan(limit);

        emptyChatIds.forEach(storedEventRepository::deleteByAggregateId);
    }

    // 👤 3. inactive users (45 days)
    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void deleteInactiveUsers() {
        LocalDateTime limit = LocalDateTime.now().minusDays(45);
        userRepository.deleteInactiveUsers(limit);
    }
}