package com.cryptomessage.server.services;

import com.cryptomessage.server.config.exceptions.ConflictException;
import com.cryptomessage.server.model.dto.security.authentication.AppUserDTO;
import com.cryptomessage.server.model.persistance.user.AppUser;
import com.cryptomessage.server.repositories.UserRepository;
import org.bouncycastle.operator.OperatorCreationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.security.*;
import java.util.NoSuchElementException;

@Service
public class AuthenticationService {

    private final JwtService jwtService;
    private final RSAEncryptionWithPassphraseService rsaEncryptionWithPassphraseService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsServiceImp userDetailsServiceImp;

    private final UserRepository userRepository;

    public AuthenticationService(
            JwtService jwtService,
            RSAEncryptionWithPassphraseService rsaEncryptionWithPassphraseService,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            UserDetailsServiceImp userDetailsServiceImp
    ) {
        this.jwtService = jwtService;
        this.rsaEncryptionWithPassphraseService = rsaEncryptionWithPassphraseService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.userDetailsServiceImp = userDetailsServiceImp;
    }

    public void createUser(String username, String passphrase)
            throws
            NoSuchAlgorithmException,
            NoSuchProviderException,
            IOException,
            OperatorCreationException {
        if (isUserExisting(username)) {
            throw new ConflictException("User already exists");
        }
        userRepository.save(buildNewUser(username, passphrase));
    }

    public AppUserDTO authenticate(String username, String passphrase)  {
        // Verificar si el usuario existe y autenticar
        return userRepository.findUserByUsername(username)
                .map(appUser -> {
                    try {
                        return performAuthentication(appUser, passphrase);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .orElseThrow(() -> new NoSuchElementException("User not found"));
    }


    public AppUserDTO verifyToken(String token) {
        return userRepository.findUserByUsername(jwtService.extractUsername(token))
                .filter(appUser -> isThisTokenValid(token, appUser))
                .map(appUser -> {
                    try {
                        return buildAuthenticationResponse(appUser, token, jwtService.extractPassphrase(token));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .orElseThrow(() -> new NoSuchElementException("User not found"));
    }

    private boolean isThisTokenValid(String token, AppUser appUser) {
        if (!jwtService.isTokenValid(token, userDetailsServiceImp.loadUserByAppUser(appUser))) {
            throw new IllegalArgumentException("Invalid token");
        }
        return true;
    }

    private AppUserDTO performAuthentication(AppUser appUser, String passphrase) throws Exception {
        // Autenticar al usuario
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        appUser.getUsername(),
                        passphrase
                )
        );
        String token = jwtService.generateToken(appUser, passphrase);
        return buildAuthenticationResponse(appUser, token, passphrase);
    }

    private boolean isUserExisting(String username) {
        return userRepository.findUserByUsername(username).isPresent();
    }

    private AppUser buildNewUser(String username, String passphrase)
            throws NoSuchAlgorithmException, NoSuchProviderException, IOException, OperatorCreationException {

        // Generar un par de claves (PublicKey y PrivateKey)
        KeyPair keyPair = rsaEncryptionWithPassphraseService.generateKeyPair();

        // Obtener la clave pública y privada
        PublicKey publicKey = keyPair.getPublic();
        PrivateKey privateKey = keyPair.getPrivate();

        // Cifrar la clave privada con la passphrase
        String encryptedPrivateKey = rsaEncryptionWithPassphraseService.savePrivateKeyWithPassphrase(privateKey, passphrase);

        // Generar el hash de la passphrase
        String passphraseHash = passwordEncoder.encode(passphrase);

        // Construir el objeto AppUser
        return AppUser.builder()
                .username(username)
                .passphraseHash(passphraseHash)
                .publicKey(publicKey) // Usar el objeto PublicKey directamente
                .encryptedPrivateKey(encryptedPrivateKey) // La clave privada encriptada sigue siendo String
                .build();
    }


    private AppUserDTO buildAuthenticationResponse(AppUser appUser, String token, String passphrase) throws Exception {
        return AppUserDTO.builder()
                .userId(appUser.getUserId())
                .token(token)
                .username(appUser.getUsername())
                .publicKey(appUser.getPublicKey())
                .privateKey(
                        rsaEncryptionWithPassphraseService.decryptPrivateKey(
                                appUser.getEncryptedPrivateKey(),
                                passphrase
                        )
                )
                .createdAt(appUser.getCreatedAt().toString())
                .contacts(appUser.getContactsAsDTOs())
                .chats(appUser.getChatsAsDTOs(appUser.getUserId()))
                .build();
    }
}

//0
//@Service
//public class AuthenticationService {
//
//    private final JwtService jwtService;
//    private final RSAEncryptionWithPassphraseService rsaEncryptionWithPassphraseService;
//    private final PasswordEncoder passwordEncoder;
//    private final AuthenticationManager authenticationManager;
//    private final UserDetailsServiceImp userDetailsServiceImp;
//
//    private final UserRepository userRepository;
//
//    public AuthenticationService(
//            JwtService jwtService,
//            RSAEncryptionWithPassphraseService rsaEncryptionWithPassphraseService,
//            UserRepository userRepository,
//            PasswordEncoder passwordEncoder,
//            AuthenticationManager authenticationManager,
//            UserDetailsServiceImp userDetailsServiceImp
//    ) {
//        this.jwtService = jwtService;
//        this.rsaEncryptionWithPassphraseService = rsaEncryptionWithPassphraseService;
//        this.userRepository = userRepository;
//        this.passwordEncoder = passwordEncoder;
//        this.authenticationManager = authenticationManager;
//        this.userDetailsServiceImp = userDetailsServiceImp;
//    }
//
//    public void createUser(String username, String passphrase)
//            throws
//            NoSuchAlgorithmException,
//            NoSuchProviderException,
//            IOException,
//            OperatorCreationException {
//        if (isUserExisting(username)) {
//            throw new ConflictException("User already exists");
//        }
//        userRepository.save(buildNewUser(username, passphrase));
//    }
//
//    public AuthenticationResponse authenticate(String username, String passphrase) {
//        // Verificar si el usuario existe y autenticar
//        return userRepository.findUserByUsername(username)
//                .map(appUser -> performAuthentication(appUser, passphrase))
//                .orElseThrow(() -> new NoSuchElementException("User not found"));
//    }
//
//
//    public AuthenticationResponse verifyToken(String token) {
//        return userRepository.findUserByUsername(jwtService.extractUsername(token))
//                .filter(appUser -> isThisTokenValid(token, appUser))
//                .map(appUser -> buildAuthenticationResponse(appUser, token))
//                .orElseThrow(() -> new NoSuchElementException("User not found"));
//    }
//
//    private boolean isThisTokenValid(String token, AppUser appUser) {
//        if (!jwtService.isTokenValid(token, userDetailsServiceImp.loadUserByAppUser(appUser))) {
//            throw new IllegalArgumentException("Invalid token");
//        }
//        return true;
//    }
//
//    private AuthenticationResponse performAuthentication(AppUser appUser, String passphrase) {
//        // Autenticar al usuario
//        authenticationManager.authenticate(
//                new UsernamePasswordAuthenticationToken(
//                        appUser.getUsername(),
//                        passphrase
//                )
//        );
//        String token = jwtService.generateToken(appUser);
//        return buildAuthenticationResponse(appUser, token);
//    }
//
//    private boolean isUserExisting(String username) {
//        return userRepository.findUserByUsername(username).isPresent();
//    }
//
//    private AppUser buildNewUser(String username, String passphrase) throws
//            NoSuchAlgorithmException,
//            NoSuchProviderException,
//            IOException,
//            OperatorCreationException {
//        KeyPair keyPair = rsaEncryptionWithPassphraseService.generateKeyPair();
//        String publicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
//        String encryptedPrivateKey = rsaEncryptionWithPassphraseService.savePrivateKeyWithPassphrase(keyPair.getPrivate(), passphrase);
//        String passphraseHash = passwordEncoder.encode(passphrase);
//
//        return AppUser.builder()
//                .username(username)
//                .passphraseHash(passphraseHash)
//                .publicKey(publicKey)
//                .encryptedPrivateKey(encryptedPrivateKey)
//                .build();
//    }
//
//    private AuthenticationResponse buildAuthenticationResponse(AppUser appUser, String token) {
//        return AuthenticationResponse.builder()
//                .userId(appUser.getUserId())
//                .token(token)
//                .username(appUser.getUsername())
//                .publicKey(appUser.getPublicKey())
//                .privateKey(appUser.getEncryptedPrivateKey())
//                .createdAt(appUser.getCreatedAt().toString())
//                .contacts(appUser.getContactsAsDTOs())
//                .chats(appUser.getChatsAsDTOs())
//                .build();
//    }
//}