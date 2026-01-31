package com.cryptomessage.server.model.mapper;

import com.cryptomessage.server.model.dto.chat.ChatResponse;
import com.cryptomessage.server.model.entity.chat.Chat;
import com.cryptomessage.server.model.entity.user.AppUser;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;

@Service
public class ChatMapper {

    private final UserMapper userMapper;

    public ChatMapper(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public ChatResponse toResponse(Chat chat, AppUser currentUser) {
        if (chat == null) return null;

        AppUser other =
                chat.getAppUser1().equals(currentUser)
                        ? chat.getAppUser2()
                        : chat.getAppUser1();

        return new ChatResponse(
                chat.getChatId(),
                userMapper.toResponse(other),
                chat.getCreatedAt().toInstant(ZoneOffset.UTC)
        );
    }
}

