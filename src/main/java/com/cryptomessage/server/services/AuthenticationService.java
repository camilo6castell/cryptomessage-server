package com.cryptomessage.server.services;

import com.cryptomessage.server.model.dto.security.authentication.UserResponse;
import com.cryptomessage.server.model.entity.user.AppUser;
import com.cryptomessage.server.model.mapper.UserMapper;
import com.cryptomessage.server.repositories.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import java.security.*;

@Service
public class AuthenticationService {

    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public AuthenticationService(
            JwtService jwtService,
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            UserMapper userMapper
    ) {
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    public UserResponse authenticate(String username, String passphrase) {

        authenticateWithSpring(username, passphrase);

        AppUser user = userRepository.findUserByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        String token = jwtService.generateToken(user);

        return userMapper.toResponse(user, token);
    }

    public UserResponse verifyToken() {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user");
        }

        String username = auth.getName();

        AppUser user = userRepository.findUserByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        String token = (String) auth.getCredentials(); // 👈 opcional, depende del filtro

        return userMapper.toResponse(user, token);
    }

    private void authenticateWithSpring(String username, String passphrase) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, passphrase)
        );
    }
}
