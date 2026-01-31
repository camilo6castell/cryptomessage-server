package com.cryptomessage.server.services;

import com.cryptomessage.server.model.entity.user.AppUser;
import com.cryptomessage.server.repositories.UserRepository;
import org.springframework.stereotype.Service;
import java.util.NoSuchElementException;

@Service
public class CryptoService {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    public CryptoService(
            UserRepository userRepository,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    public AppUser resolveUserFromToken(String bearerToken) {

        String token = jwtService.stripBearer(bearerToken);
        String username = jwtService.extractUsername(token);

        return userRepository.findUserByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("User not found"));
    }
}
