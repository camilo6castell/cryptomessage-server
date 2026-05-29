package com.cryptomessage.server.services;

import com.cryptomessage.server.config.exceptions.ConflictException;
import com.cryptomessage.server.model.entity.user.AppUser;
import com.cryptomessage.server.repositories.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.KeyPair;

@Service
public class UserRegistrationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserRegistrationService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void createUser(
            String username,
            String passphrase,
            String publicKey,
            String encryptedPrivateKey
    ) {

        if (userRepository.findUserByUsername(username).isPresent()) {
            throw new ConflictException("User already exists");
        }

        AppUser user = AppUser.builder()
                .username(username)
                .passphraseHash(passwordEncoder.encode(passphrase))
                .publicKey(publicKey) // 🔥 ahora es String (base64)
                .encryptedPrivateKey(encryptedPrivateKey)
                .build();

        userRepository.save(user);
    }
}

