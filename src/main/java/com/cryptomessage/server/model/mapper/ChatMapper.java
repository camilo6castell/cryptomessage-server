package com.cryptomessage.server.model.mapper;

import com.cryptomessage.server.model.dto.chat.ChatResponse;
import com.cryptomessage.server.model.entity.chat.Chat;
import com.cryptomessage.server.model.entity.user.AppUser;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;

@Service
public class ChatMapper {

    private final UserMapper userMapper;

    public ChatMapper(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public ChatResponse toResponse(Chat chat, AppUser requester) {

        AppUser other = chat.getOtherParticipant(requester.getUserId());
        Instant createdAt = chat.getCreatedAt() != null
                ? chat.getCreatedAt().toInstant(ZoneOffset.UTC)
                : null;


        return new ChatResponse(
                chat.getChatId(),
                chat.getStatus(),
                userMapper.toResponse(other),
                createdAt
        );
    }

}

