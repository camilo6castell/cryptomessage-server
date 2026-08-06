package com.cryptomessage.server.application.chat;

import com.cryptomessage.server.domain.chat.Chat;
import com.cryptomessage.server.domain.chat.ChatId;
import com.cryptomessage.server.domain.chat.port.ChatAggregateRepository;
import com.cryptomessage.server.domain.chat.port.NotificationPort;
import com.cryptomessage.server.domain.generic.DomainEvent;
import com.cryptomessage.server.domain.shared.UserId;
import com.cryptomessage.server.infrastructure.projection.ChatProjector;
import com.cryptomessage.server.model.entity.user.AppUser;
import com.cryptomessage.server.model.mapper.ChatMapper;
import com.cryptomessage.server.repositories.ChatRepository;
import com.cryptomessage.server.services.CurrentUserService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Component
public class AcceptChatUseCase {

    private final CurrentUserService currentUserService;
    private final ChatAggregateRepository chatAggregateRepository;
    private final ChatProjector chatProjector;
    private final ChatRepository chatReadRepository;
    private final ChatMapper chatMapper;
    private final NotificationPort notificationPort;

    public AcceptChatUseCase(
            CurrentUserService currentUserService,
            ChatAggregateRepository chatAggregateRepository,
            ChatProjector chatProjector,
            ChatRepository chatReadRepository,
            ChatMapper chatMapper,
            NotificationPort notificationPort
    ) {
        this.currentUserService = currentUserService;
        this.chatAggregateRepository = chatAggregateRepository;
        this.chatProjector = chatProjector;
        this.chatReadRepository = chatReadRepository;
        this.chatMapper = chatMapper;
        this.notificationPort = notificationPort;
    }

    @Transactional
    public void execute(Long chatId) {
        AppUser user = currentUserService.get();
        ChatId id = ChatId.of(String.valueOf(chatId));

        Chat chat = chatAggregateRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Chat not found"));

        chat.accept(UserId.of(user.getUserId()));

        List<DomainEvent> newEvents = chat.getUncommittedChanges();
        chatAggregateRepository.save(chat);
        chatProjector.project(chatId, newEvents);

        var chatRow = chatReadRepository.findById(chatId).orElseThrow();
        AppUser user1 = chatRow.getAppUser1();
        AppUser user2 = chatRow.getAppUser2();

        notificationPort.notifyUser(user1.getUsername(), "/queue/chats", chatMapper.toResponse(chatRow, user1));
        notificationPort.notifyUser(user2.getUsername(), "/queue/chats", chatMapper.toResponse(chatRow, user2));
    }
}
