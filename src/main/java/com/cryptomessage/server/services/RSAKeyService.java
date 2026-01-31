package com.cryptomessage.server.services;

import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

@Service
public class RSAKeyService {

    private static final String PROVIDER = "BC";
    private static final String ALGORITHM = "RSA";
    private static final String KEY_CIPHER = "AES/GCM/NoPadding";

    public KeyPair generateKeyPair() throws GeneralSecurityException {
        KeyPairGenerator generator =
                KeyPairGenerator.getInstance(ALGORITHM, PROVIDER);
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    /* ================= PRIVATE KEY PROTECTION ================= */

    public String encryptPrivateKey(PrivateKey privateKey, String passphrase)
            throws GeneralSecurityException {

        byte[] keyBytes = privateKey.getEncoded();
        SecretKey aesKey = deriveAesKey(passphrase);

        Cipher cipher = Cipher.getInstance(KEY_CIPHER);
        cipher.init(Cipher.ENCRYPT_MODE, aesKey);

        byte[] encrypted = cipher.doFinal(keyBytes);
        return Base64.getEncoder().encodeToString(encrypted);
    }

    public PrivateKey decryptPrivateKey(String encryptedPrivateKey, String passphrase)
            throws GeneralSecurityException {

        byte[] encryptedBytes = Base64.getDecoder().decode(encryptedPrivateKey);
        SecretKey aesKey = deriveAesKey(passphrase);

        Cipher cipher = Cipher.getInstance(KEY_CIPHER);
        cipher.init(Cipher.DECRYPT_MODE, aesKey);

        byte[] decodedKey = cipher.doFinal(encryptedBytes);

        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decodedKey);
        return KeyFactory.getInstance(ALGORITHM, PROVIDER)
                .generatePrivate(spec);
    }

    /* ================= INTERNAL ================= */

    private SecretKey deriveAesKey(String passphrase)
            throws GeneralSecurityException {

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = digest.digest(
                passphrase.getBytes(StandardCharsets.UTF_8)
        );

        return new SecretKeySpec(keyBytes, 0, 32, "AES");
    }
}

