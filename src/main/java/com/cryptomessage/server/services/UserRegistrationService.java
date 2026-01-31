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
    private final RSAKeyService rsaKeyService;
    private final RSAEncryptionService rsaEncryptionService;
    private final PasswordEncoder passwordEncoder;

    public UserRegistrationService(
            UserRepository userRepository,
            RSAKeyService rsaKeyService,
            RSAEncryptionService rsaEncryptionService,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.rsaKeyService = rsaKeyService;
        this.rsaEncryptionService = rsaEncryptionService;
        this.passwordEncoder = passwordEncoder;
    }

    public void createUser(String username, String passphrase) throws Exception {

        if (userRepository.findUserByUsername(username).isPresent()) {
            throw new ConflictException("User already exists");
        }

        KeyPair keyPair = rsaKeyService.generateKeyPair();

        String encryptedPrivateKey =
                rsaEncryptionService.encryptPrivateKey(
                        keyPair.getPrivate(),
                        passphrase
                );

        AppUser user = AppUser.builder()
                .username(username)
                .passphraseHash(passwordEncoder.encode(passphrase))
                .publicKey(keyPair.getPublic())
                .encryptedPrivateKey(encryptedPrivateKey)
                .build();

        userRepository.save(user);
    }
}

