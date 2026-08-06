package com.cryptomessage.server.application.message;

import com.cryptomessage.server.domain.chat.Chat;
import com.cryptomessage.server.domain.chat.ChatId;
import com.cryptomessage.server.domain.chat.events.MessageSent;
import com.cryptomessage.server.domain.chat.port.ChatAggregateRepository;
import com.cryptomessage.server.domain.chat.port.NotificationPort;
import com.cryptomessage.server.domain.generic.DomainEvent;
import com.cryptomessage.server.domain.shared.UserId;
import com.cryptomessage.server.infrastructure.projection.ChatProjector;
import com.cryptomessage.server.model.dto.message.MessageResponse;
import com.cryptomessage.server.model.dto.message.SendMessageRequest;
import com.cryptomessage.server.model.entity.message.Message;
import com.cryptomessage.server.model.entity.user.AppUser;
import com.cryptomessage.server.model.mapper.MessageMapper;
import com.cryptomessage.server.services.CurrentUserService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Component
public class SendMessageUseCase {

    private final CurrentUserService currentUserService;
    private final ChatAggregateRepository chatAggregateRepository;
    private final ChatProjector chatProjector;
    private final MessageMapper messageMapper;
    private final NotificationPort notificationPort;

    public SendMessageUseCase(
            CurrentUserService currentUserService,
            ChatAggregateRepository chatAggregateRepository,
            ChatProjector chatProjector,
            MessageMapper messageMapper,
            NotificationPort notificationPort
    ) {
        this.currentUserService = currentUserService;
        this.chatAggregateRepository = chatAggregateRepository;
        this.chatProjector = chatProjector;
        this.messageMapper = messageMapper;
        this.notificationPort = notificationPort;
    }

    @Transactional
    public MessageResponse execute(SendMessageRequest request) {
        AppUser sender = currentUserService.get();
        ChatId chatId = ChatId.of(String.valueOf(request.chatId()));

        Chat chat = chatAggregateRepository.findById(chatId)
                .orElseThrow(() -> new NoSuchElementException("Chat not found"));

        Map<UserId, String> encryptedContentByUser = request.encryptedContentByUser().entrySet().stream()
                .collect(Collectors.toMap(entry -> UserId.of(entry.getKey()), Map.Entry::getValue));

        chat.sendMessage(UserId.of(sender.getUserId()), encryptedContentByUser);

        List<DomainEvent> newEvents = chat.getUncommittedChanges();
        chatAggregateRepository.save(chat);

        // sendMessage() always yields exactly one event — see Chat#sendMessage.
        MessageSent event = (MessageSent) newEvents.get(0);
        Message message = chatProjector.projectMessageSent(request.chatId(), event);

        AppUser recipient = message.getChat().getOtherParticipant(sender.getUserId());

        MessageResponse responseForSender = messageMapper.toResponse(
                message, request.encryptedContentByUser().get(sender.getUserId())
        );
        MessageResponse responseForRecipient = messageMapper.toResponse(
                message, request.encryptedContentByUser().get(recipient.getUserId())
        );

        notificationPort.notifyUser(recipient.getUsername(), "/queue/messages", responseForRecipient);
        notificationPort.notifyUser(sender.getUsername(), "/queue/messages", responseForSender);

        return responseForSender;
    }
}
