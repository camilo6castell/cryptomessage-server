package com.cryptomessage.server.model.mapper;

import com.cryptomessage.server.model.dto.security.authentication.UserResponse;
import com.cryptomessage.server.model.entity.user.AppUser;
import org.springframework.stereotype.Service;
import java.time.ZoneOffset;

@Service
public class UserMapper {

    public UserResponse toResponse(AppUser user, String token) {
        if (user == null) return null;

       return new UserResponse(
                user.getUserId(),
                user.getUsername(),
                token,
                user.getPublicKey(),
                user.getEncryptedPrivateKey(),
                user.getCreatedAt().toInstant(ZoneOffset.UTC)
        );
    }
}
