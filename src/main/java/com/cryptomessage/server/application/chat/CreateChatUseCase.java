package com.cryptomessage.server.application.chat;

import com.cryptomessage.server.config.exceptions.ConflictException;
import com.cryptomessage.server.domain.chat.Chat;
import com.cryptomessage.server.domain.chat.ChatId;
import com.cryptomessage.server.domain.chat.port.ChatAggregateRepository;
import com.cryptomessage.server.domain.chat.port.ChatIdGenerator;
import com.cryptomessage.server.domain.chat.port.NotificationPort;
import com.cryptomessage.server.domain.generic.DomainEvent;
import com.cryptomessage.server.domain.shared.UserId;
import com.cryptomessage.server.infrastructure.projection.ChatProjector;
import com.cryptomessage.server.model.dto.chat.ChatResponse;
import com.cryptomessage.server.model.entity.user.AppUser;
import com.cryptomessage.server.model.mapper.ChatMapper;
import com.cryptomessage.server.repositories.UserRepository;
import com.cryptomessage.server.services.CurrentUserService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Component
public class CreateChatUseCase {

    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;
    private final ChatIdGenerator chatIdGenerator;
    private final ChatAggregateRepository chatAggregateRepository;
    private final ChatProjector chatProjector;
    private final com.cryptomessage.server.repositories.ChatRepository chatReadRepository;
    private final ChatMapper chatMapper;
    private final NotificationPort notificationPort;

    public CreateChatUseCase(
            CurrentUserService currentUserService,
            UserRepository userRepository,
            ChatIdGenerator chatIdGenerator,
            ChatAggregateRepository chatAggregateRepository,
            ChatProjector chatProjector,
            com.cryptomessage.server.repositories.ChatRepository chatReadRepository,
            ChatMapper chatMapper,
            NotificationPort notificationPort
    ) {
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
        this.chatIdGenerator = chatIdGenerator;
        this.chatAggregateRepository = chatAggregateRepository;
        this.chatProjector = chatProjector;
        this.chatReadRepository = chatReadRepository;
        this.chatMapper = chatMapper;
        this.notificationPort = notificationPort;
    }

    @Transactional
    public ChatResponse execute(String username) {
        AppUser owner = currentUserService.get();
        AppUser otherUser = userRepository.findUserByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        if (owner.getUserId().equals(otherUser.getUserId())) {
            throw new ConflictException("Cannot create chat with yourself");
        }

        ChatId chatId = chatIdGenerator.nextId();
        Chat chat = Chat.create(
                chatId,
                UserId.of(owner.getUserId()),
                UserId.of(otherUser.getUserId()),
                UserId.of(owner.getUserId())
        );

        List<DomainEvent> newEvents = chat.getUncommittedChanges();
        chatAggregateRepository.save(chat);

        Long readModelChatId = Long.valueOf(chatId.value());
        try {
            chatProjector.project(readModelChatId, newEvents);
        } catch (DataIntegrityViolationException e) {
            // Same race the original code guarded against: two chats between the
            // same pair of users, both racing on the `uk_chats_users` constraint.
            throw new ConflictException("Chat already exists");
        }

        var chatRow = chatReadRepository.findById(readModelChatId).orElseThrow();

        notificationPort.notifyUser(
                otherUser.getUsername(), "/queue/chats",
                chatMapper.toResponse(chatRow, otherUser)
        );

        return chatMapper.toResponse(chatRow, owner);
    }
}
