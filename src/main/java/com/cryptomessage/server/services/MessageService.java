package com.cryptomessage.server.services;

import com.cryptomessage.server.model.dto.message.SendMessageRequest;
import com.cryptomessage.server.model.dto.message.MessageResponse;
import com.cryptomessage.server.model.entity.chat.Chat;
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

    /* ================= SEND MESSAGE ================= */

    @Transactional
    public MessageResponse sendMessage(
            String bearerToken,
            SendMessageRequest request
    ) throws Exception {

        AppUser sender = cryptoService.resolveUserFromToken(bearerToken);

        Chat chat = chatRepository.findById(request.getChatId())
                .orElseThrow(() -> new NoSuchElementException("Chat not found"));

        chat.assertUserIsParticipant(sender.getUserId());

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
}

