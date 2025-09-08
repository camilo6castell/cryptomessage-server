package com.cryptomessage.server.services;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;

import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class RSAEncryptionService {
    private static final String RSA = "RSA";

    static {
        // Registrar el proveedor de BouncyCastle
        Security.addProvider(new BouncyCastleProvider());
    }

    // Generar un par de claves RSA
    public KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(RSA, "BC");
        keyPairGenerator.initialize(2048);
        return keyPairGenerator.generateKeyPair();
    }

    // Encriptar con la clave pública
    public String encrypt(String plainText, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance(RSA, "BC");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encryptedBytes = cipher.doFinal(plainText.getBytes());
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    // Desencriptar con la clave privada
    public String decrypt(String encryptedText, PrivateKey privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance(RSA, "BC");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
        return new String(decryptedBytes);
    }

    // Cargar una clave pública desde una cadena Base64
    public PublicKey loadPublicKey(String publicKeyStr) throws Exception {
        byte[] publicBytes = Base64.getDecoder().decode(publicKeyStr);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(RSA, "BC");
        return keyFactory.generatePublic(keySpec);
    }

    // Cargar una clave privada desde una cadena Base64
    public PrivateKey loadPrivateKey(String privateKeyStr) throws Exception {
        byte[] privateBytes = Base64.getDecoder().decode(privateKeyStr);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(RSA, "BC");
        return keyFactory.generatePrivate(keySpec);
    }
}
