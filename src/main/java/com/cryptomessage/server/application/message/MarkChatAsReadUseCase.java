package com.cryptomessage.server.application.message;

import com.cryptomessage.server.domain.chat.Chat;
import com.cryptomessage.server.domain.chat.ChatId;
import com.cryptomessage.server.domain.chat.port.ChatAggregateRepository;
import com.cryptomessage.server.domain.generic.DomainEvent;
import com.cryptomessage.server.domain.shared.UserId;
import com.cryptomessage.server.infrastructure.projection.ChatProjector;
import com.cryptomessage.server.model.entity.user.AppUser;
import com.cryptomessage.server.services.CurrentUserService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Component
public class MarkChatAsReadUseCase {

    private final CurrentUserService currentUserService;
    private final ChatAggregateRepository chatAggregateRepository;
    private final ChatProjector chatProjector;

    public MarkChatAsReadUseCase(
            CurrentUserService currentUserService,
            ChatAggregateRepository chatAggregateRepository,
            ChatProjector chatProjector
    ) {
        this.currentUserService = currentUserService;
        this.chatAggregateRepository = chatAggregateRepository;
        this.chatProjector = chatProjector;
    }

    @Transactional
    public void execute(Long chatId) {
        AppUser user = currentUserService.get();
        ChatId id = ChatId.of(String.valueOf(chatId));

        Chat chat = chatAggregateRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Chat not found"));

        chat.markAsRead(UserId.of(user.getUserId()));

        List<DomainEvent> newEvents = chat.getUncommittedChanges();
        chatAggregateRepository.save(chat);
        chatProjector.project(chatId, newEvents);
    }
}
