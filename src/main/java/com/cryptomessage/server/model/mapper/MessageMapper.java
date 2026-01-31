package com.cryptomessage.server.model.mapper;

import com.cryptomessage.server.model.dto.message.MessageResponse;
import com.cryptomessage.server.model.entity.message.Message;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;

@Service
public class MessageMapper {

    public MessageResponse toResponse(
            Message message,
            String encryptedContentForViewer
    ) {
        if (message == null) return null;

        return new MessageResponse(
                message.getMessageId(),
                message.getChat().getChatId(),
                message.getSender().getUserId(),
                encryptedContentForViewer,
                message.isRead(),
                message.getSentAt().toInstant(ZoneOffset.UTC)
        );
    }
}


