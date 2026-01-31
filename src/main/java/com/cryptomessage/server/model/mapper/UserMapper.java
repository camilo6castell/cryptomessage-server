package com.cryptomessage.server.model.mapper;

import com.cryptomessage.server.model.dto.security.authentication.UserResponse;
import com.cryptomessage.server.model.entity.user.AppUser;
import com.cryptomessage.server.services.RSAKeyConverterService;
import org.springframework.stereotype.Service;
import java.time.ZoneOffset;

@Service
public class UserMapper {

    private final RSAKeyConverterService rsaKeyConverter;

    public UserMapper(RSAKeyConverterService rsaKeyConverter) {
        this.rsaKeyConverter = rsaKeyConverter;
    }

    public UserResponse toResponse(AppUser user) {
        if (user == null) return null;

        return new UserResponse(
                user.getUserId(),
                user.getUsername(),
                user.getCreatedAt().toInstant(ZoneOffset.UTC),
                rsaKeyConverter.publicKeyToString(user.getPublicKey())
        );
    }
}
