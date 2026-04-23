package com.cryptomessage.server.services;

import com.cryptomessage.server.repositories.ChatRepository;
import com.cryptomessage.server.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import com.cryptomessage.server.repositories.MessageRepository;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;

@Service
public class Scheduler {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ChatRepository chatRepository;

    public Scheduler(
            MessageRepository messageRepository,
            UserRepository userRepository,
            ChatRepository chatRepository
    ) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.chatRepository = chatRepository;
    }

    // 🧹 1. mensajes (15 días)
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void deleteOldMessages() {
        LocalDateTime limit = LocalDateTime.now().minusDays(15);
        messageRepository.deleteOlderThan(limit);
    }

    // 💬 2. chats vacíos (30 días)
    @Scheduled(cron = "0 15 3 * * *")
    @Transactional
    public void deleteEmptyChats() {
        LocalDateTime limit = LocalDateTime.now().minusDays(30);
        chatRepository.deleteEmptyChatsOlderThan(limit);
    }

    // 👤 3. usuarios inactivos (45 días)
    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void deleteInactiveUsers() {
        LocalDateTime limit = LocalDateTime.now().minusDays(45);
        userRepository.deleteInactiveUsers(limit);
    }
}
