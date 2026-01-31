package com.cryptomessage.server.services;

import org.springframework.stereotype.Service;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.KeySpec;
import java.util.Base64;

@Service
public class RSAEncryptionService {

    /* ================= RSA (MENSAJES) ================= */

    private static final String PROVIDER = "BC";
    private static final String RSA_TRANSFORMATION =
            "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";

    public String encrypt(String plainText, PublicKey publicKey)
            throws GeneralSecurityException {

        Cipher cipher = Cipher.getInstance(RSA_TRANSFORMATION, PROVIDER);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);

        byte[] encrypted = cipher.doFinal(
                plainText.getBytes(StandardCharsets.UTF_8)
        );
        return Base64.getEncoder().encodeToString(encrypted);
    }

    public String decrypt(String encryptedText, PrivateKey privateKey)
            throws GeneralSecurityException {

        Cipher cipher = Cipher.getInstance(RSA_TRANSFORMATION, PROVIDER);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);

        byte[] decrypted = cipher.doFinal(
                Base64.getDecoder().decode(encryptedText)
        );
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    /* ================= PRIVATE KEY + PASSPHRASE ================= */

    private static final String AES_ALGORITHM = "AES";
    private static final String AES_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String KDF_ALGORITHM = "PBKDF2WithHmacSHA256";

    private static final int SALT_LENGTH = 16;
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH = 128;
    private static final int ITERATIONS = 65_536;
    private static final int KEY_LENGTH = 256;

    public String encryptPrivateKey(PrivateKey privateKey, String passphrase)
            throws GeneralSecurityException {

        byte[] salt = SecureRandom.getInstanceStrong().generateSeed(SALT_LENGTH);
        byte[] iv = SecureRandom.getInstanceStrong().generateSeed(IV_LENGTH);

        SecretKey aesKey = deriveKey(passphrase, salt);

        Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(TAG_LENGTH, iv));

        byte[] encryptedKey = cipher.doFinal(privateKey.getEncoded());

        ByteBuffer buffer = ByteBuffer.allocate(
                salt.length + iv.length + encryptedKey.length
        );
        buffer.put(salt);
        buffer.put(iv);
        buffer.put(encryptedKey);

        return Base64.getEncoder().encodeToString(buffer.array());
    }

    public PrivateKey decryptPrivateKey(String encryptedPrivateKey, String passphrase)
            throws GeneralSecurityException {

        byte[] decoded = Base64.getDecoder().decode(encryptedPrivateKey);
        ByteBuffer buffer = ByteBuffer.wrap(decoded);

        byte[] salt = new byte[SALT_LENGTH];
        byte[] iv = new byte[IV_LENGTH];
        byte[] encryptedKey = new byte[buffer.remaining() - SALT_LENGTH - IV_LENGTH];

        buffer.get(salt);
        buffer.get(iv);
        buffer.get(encryptedKey);

        SecretKey aesKey = deriveKey(passphrase, salt);

        Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, aesKey, new GCMParameterSpec(TAG_LENGTH, iv));

        byte[] privateKeyBytes = cipher.doFinal(encryptedKey);

        return KeyFactory.getInstance("RSA")
                .generatePrivate(new java.security.spec.PKCS8EncodedKeySpec(privateKeyBytes));
    }

    private SecretKey deriveKey(String passphrase, byte[] salt)
            throws GeneralSecurityException {

        SecretKeyFactory factory = SecretKeyFactory.getInstance(KDF_ALGORITHM);
        KeySpec spec = new PBEKeySpec(
                passphrase.toCharArray(),
                salt,
                ITERATIONS,
                KEY_LENGTH
        );

        return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), AES_ALGORITHM);
    }
}
