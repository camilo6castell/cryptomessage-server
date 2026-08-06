package com.cryptomessage.server.application.message;

import com.cryptomessage.server.model.dto.message.MessageResponse;
import com.cryptomessage.server.model.entity.chat.Chat;
import com.cryptomessage.server.model.entity.user.AppUser;
import com.cryptomessage.server.model.mapper.MessageMapper;
import com.cryptomessage.server.repositories.ChatRepository;
import com.cryptomessage.server.repositories.MessageRepository;
import com.cryptomessage.server.services.CurrentUserService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

/** Pure read — same CQRS rationale as GetMyChatsUseCase. */
@Component
public class GetMessagesByChatUseCase {

    private final CurrentUserService currentUserService;
    private final ChatRepository chatReadRepository;
    private final MessageRepository messageRepository;
    private final MessageMapper messageMapper;

    public GetMessagesByChatUseCase(
            CurrentUserService currentUserService,
            ChatRepository chatReadRepository,
            MessageRepository messageRepository,
            MessageMapper messageMapper
    ) {
        this.currentUserService = currentUserService;
        this.chatReadRepository = chatReadRepository;
        this.messageRepository = messageRepository;
        this.messageMapper = messageMapper;
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> execute(Long chatId) {
        AppUser user = currentUserService.get();

        Chat chat = chatReadRepository.findById(chatId)
                .orElseThrow(() -> new NoSuchElementException("Chat not found"));

        chat.assertUserIsParticipant(user.getUserId());

        return messageRepository.findByChatOrderBySentAtAsc(chat)
                .stream()
                .map(message -> messageMapper.toResponse(
                        message, message.getContentForUser(user.getUserId())
                ))
                .toList();
    }
}
