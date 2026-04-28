package com.cryptomessage.server.model.mapper;

import com.cryptomessage.server.model.dto.chat.ChatParticipantResponse;
import com.cryptomessage.server.model.entity.user.AppUser;

public class ChatParticipantMapper {
    public static ChatParticipantResponse toResponse(AppUser appUser){
        return new ChatParticipantResponse(
                appUser.getUserId(),
                appUser.getUsername(),
                appUser.getPublicKey()
        );
    }
}
