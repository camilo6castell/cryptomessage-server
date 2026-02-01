package com.cryptomessage.server.services;

import com.cryptomessage.server.config.exceptions.ForbiddenException;
import com.cryptomessage.server.model.dto.message.SendMessageRequest;
import com.cryptomessage.server.model.dto.message.MessageResponse;
import com.cryptomessage.server.model.entity.chat.Chat;
import com.cryptomessage.server.model.entity.chat.ChatStatus;
import com.cryptomessage.server.model.entity.message.Message;
import com.cryptomessage.server.model.entity.user.AppUser;
import com.cryptomessage.server.model.mapper.MessageMapper;
import com.cryptomessage.server.repositories.ChatRepository;
import com.cryptomessage.server.repositories.MessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Service
public class MessageService {

    private final CryptoService cryptoService;
    private final MessageRepository messageRepository;
    private final ChatRepository chatRepository;
    private final MessageMapper messageMapper;

    public MessageService(
            MessageRepository messageRepository,
            ChatRepository chatRepository,
            CryptoService cryptoService,
            MessageMapper messageMapper
    ) {
        this.messageRepository = messageRepository;
        this.chatRepository = chatRepository;
        this.cryptoService = cryptoService;
        this.messageMapper = messageMapper;
    }

    private void validateMessagePermission(Chat chat, AppUser sender) {

        if (chat.getStatus() == ChatStatus.BLOCKED) {
            throw new ForbiddenException("Chat blocked");
        }

        if (chat.getStatus() == ChatStatus.PENDING) {

            if (!chat.getInitiatedBy().equals(sender)) {
                throw new ForbiddenException("Chat not accepted yet");
            }

            if (chat.hasMessageFrom(sender)) {
                throw new ForbiddenException(
                        "Only one message allowed until accepted"
                );
            }
        }
    }

    /* ================= SEND MESSAGE ================= */

    @Transactional
    public MessageResponse sendMessage(
            String bearerToken,
            SendMessageRequest request
    ) {

        AppUser sender = cryptoService.resolveUserFromToken(bearerToken);

        Chat chat = chatRepository.findById(request.getChatId())
                .orElseThrow(() -> new NoSuchElementException("Chat not found"));

        chat.assertUserIsParticipant(sender.getUserId());
        validateMessagePermission(chat, sender);

        Map<Long, String> encryptedContent =
                Map.copyOf(request.getEncryptedContentByUser());

        Message message = new Message(chat, sender, encryptedContent);

        chat.addMessage(message);
        chatRepository.save(chat);

        return messageMapper.toResponse(
                message,
                encryptedContent.get(sender.getUserId())
        );
    }

    /* ================= GET MESSAGES ================= */

    @Transactional(readOnly = true)
    public List<MessageResponse> getMessagesByChat(
            String bearerToken,
            Long chatId
    ) {

        AppUser user = cryptoService.resolveUserFromToken(bearerToken);

        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new NoSuchElementException("Chat not found"));

        chat.assertUserIsParticipant(user.getUserId());

        return messageRepository.findByChatOrderBySentAtAsc(chat)
                .stream()
                .map(message ->
                        messageMapper.toResponse(
                                message,
                                message.getContentForUser(user.getUserId())
                        )
                )
                .toList();
    }

    @Transactional
    public void markChatAsRead(String bearerToken, Long chatId) {
        AppUser user = cryptoService.resolveUserFromToken(bearerToken);
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow();

        chat.assertUserIsParticipant(user.getUserId());

        messageRepository.markAsReadByChatAndNotSender(chat, user);
    }

}

