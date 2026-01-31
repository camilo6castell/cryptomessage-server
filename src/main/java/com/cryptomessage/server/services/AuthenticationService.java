package com.cryptomessage.server.services;

import com.cryptomessage.server.model.dto.security.authentication.AuthenticationResponse;
import com.cryptomessage.server.model.dto.security.authentication.LoginResponse;
import com.cryptomessage.server.model.entity.user.AppUser;
import com.cryptomessage.server.model.mapper.UserMapper;
import com.cryptomessage.server.repositories.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import java.security.*;
import java.util.NoSuchElementException;

@Service
public class AuthenticationService {

    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsServiceImp userDetailsService;
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public AuthenticationService(
            JwtService jwtService,
            AuthenticationManager authenticationManager,
            UserDetailsServiceImp userDetailsService,
            UserRepository userRepository,
            UserMapper userMapper
    ) {
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    public LoginResponse authenticate(String username, String passphrase) {

        AppUser user = userRepository.findUserByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        authenticateWithSpring(username, passphrase);

        String token = jwtService.generateToken(user);

        return new LoginResponse(
                token,
                user.getEncryptedPrivateKey(), // 🔐 CLAVE
                userMapper.toResponse(user)
        );
    }

    public AuthenticationResponse verifyToken(String bearerToken) {

        String token = jwtService.stripBearer(bearerToken);
        String username = jwtService.extractUsername(token);

        AppUser user = userRepository.findUserByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        if (!jwtService.isTokenValid(token, userDetailsService.loadUserByAppUser(user))) {
            throw new IllegalArgumentException("Invalid token");
        }

        return new AuthenticationResponse(
                token,
                userMapper.toResponse(user)
        );
    }

    private void authenticateWithSpring(String username, String passphrase) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, passphrase)
        );
    }
}
