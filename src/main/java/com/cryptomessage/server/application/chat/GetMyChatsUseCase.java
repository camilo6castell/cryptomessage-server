package com.cryptomessage.server.application.chat;

import com.cryptomessage.server.model.dto.chat.ChatResponse;
import com.cryptomessage.server.model.entity.chat.ChatStatus;
import com.cryptomessage.server.model.entity.user.AppUser;
import com.cryptomessage.server.model.mapper.ChatMapper;
import com.cryptomessage.server.repositories.ChatRepository;
import com.cryptomessage.server.services.CurrentUserService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Pure read — CQRS's "Q" side. Queries the projection directly, never touches
 * the event store or reconstructs the aggregate. There is no business rule to
 * enforce when just listing chats.
 */
@Component
public class GetMyChatsUseCase {

    private final CurrentUserService currentUserService;
    private final ChatRepository chatReadRepository;
    private final ChatMapper chatMapper;

    public GetMyChatsUseCase(
            CurrentUserService currentUserService,
            ChatRepository chatReadRepository,
            ChatMapper chatMapper
    ) {
        this.currentUserService = currentUserService;
        this.chatReadRepository = chatReadRepository;
        this.chatMapper = chatMapper;
    }

    @Transactional(readOnly = true)
    public List<ChatResponse> execute(ChatStatus status) {
        AppUser owner = currentUserService.get();

        var chats = (status == null)
                ? chatReadRepository.findByAppUser1OrAppUser2(owner, owner)
                : chatReadRepository.findByUserAndStatus(owner, status);

        return chats.stream()
                .map(chat -> chatMapper.toResponse(chat, owner))
                .toList();
    }
}
