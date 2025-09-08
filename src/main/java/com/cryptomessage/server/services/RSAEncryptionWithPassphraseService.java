package com.cryptomessage.server.services;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.pkcs.RSAPublicKey;
import org.bouncycastle.crypto.engines.RSAEngine;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.PKCS8Generator;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8EncryptorBuilder;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.OutputEncryptor;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;

import org.bouncycastle.pkcs.jcajce.JcePKCSPBEInputDecryptorProviderBuilder;
import org.springframework.stereotype.Service;
import javax.crypto.Cipher;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Service
public class RSAEncryptionWithPassphraseService {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * Generates an RSA key pair using BouncyCastle provider.
     *
     * @return KeyPair containing RSA public and private keys
     * @throws NoSuchAlgorithmException, NoSuchProviderException if the algorithm or provider is not available
     */
    // Generar un par de claves RSA
    public KeyPair generateKeyPair() throws NoSuchAlgorithmException, NoSuchProviderException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", "BC");
        keyPairGenerator.initialize(2048);
        return keyPairGenerator.generateKeyPair();
    }

    /**
     * Saves the private key with a passphrase in PKCS8 format.
     *
     * @param privateKey the private key to be saved
     * @param passphrase the passphrase to encrypt the private key
     * @return the private key in PKCS8 format as a String
     * @throws IOException, OperatorCreationException if an error occurs during the process
     */
    // Guardar clave privada con passphrase en formato PKCS8
    public String savePrivateKeyWithPassphrase(PrivateKey privateKey, String passphrase) throws IOException, OperatorCreationException {
        StringWriter privateKeyStringWriter = new StringWriter();
        try (JcaPEMWriter pemWriter = new JcaPEMWriter(privateKeyStringWriter)) {
            JceOpenSSLPKCS8EncryptorBuilder encryptorBuilder = new JceOpenSSLPKCS8EncryptorBuilder(PKCS8Generator.PBE_SHA1_3DES);
            encryptorBuilder.setPassword(passphrase.toCharArray());
            encryptorBuilder.setIterationCount(10000); // Incrementa para mayor seguridad
            OutputEncryptor encryptor = encryptorBuilder.build();

            pemWriter.writeObject(new PKCS8Generator(PrivateKeyInfo.getInstance(privateKey.getEncoded()), encryptor));
        }
        return privateKeyStringWriter.toString();
    }

    /**
     * Decrypts an encrypted private key using a passphrase.
     *
     * @param encryptedPrivateKey PEM formatted string of the encrypted private key
     * @param passphrase          the passphrase used to encrypt the private key
     * @return the decrypted PrivateKey object
     * @throws Exception if the passphrase is incorrect or an error occurs during decryption
     */
    public PrivateKey decryptPrivateKey(String encryptedPrivateKey, String passphrase) throws Exception {
        try (StringReader stringReader = new StringReader(encryptedPrivateKey);
             PEMParser pemParser = new PEMParser(stringReader)) {

            Object parsedObject = pemParser.readObject();
            if (!(parsedObject instanceof PKCS8EncryptedPrivateKeyInfo encryptedInfo)) {
                throw new IllegalArgumentException("Invalid private key format.");
            }

            // Construcción del decryptor usando la passphrase
            JcePKCSPBEInputDecryptorProviderBuilder decryptorProviderBuilder =
                    new JcePKCSPBEInputDecryptorProviderBuilder();
            PrivateKeyInfo privateKeyInfo = encryptedInfo.decryptPrivateKeyInfo(
                    decryptorProviderBuilder.build(passphrase.toCharArray()));

            // Conversión a un objeto PrivateKey
            return new JcaPEMKeyConverter().getPrivateKey(privateKeyInfo);
        }
    }

    /**
     * Encrypts data using the provided RSA public key.
     *
     * @param data      the data to be encrypted
     * @param publicKey the RSA public key
     * @return the encrypted data as a Base64 encoded String
     * @throws GeneralSecurityException if an error occurs during encryption
     */

    // /////////////////////////////// PARA CONVERSACIONES

    // A OTROS

    // Encriptar datos con clave pública
    public String encrypt(String data, PublicKey publicKey) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("RSA", "BC");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encryptedBytes = cipher.doFinal(data.getBytes());
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    /**
     * Decrypts data using the provided RSA private key.
     *
     * @param encryptedData the encrypted data as a Base64 encoded String
     * @param privateKey    the RSA private key
     * @return the decrypted data as a String
     * @throws GeneralSecurityException if an error occurs during decryption
     */
    // Desencriptar datos con clave privada (protegida con passphrase)
    public String decrypt(String encryptedData, PrivateKey privateKey) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("RSA", "BC");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
        return new String(decryptedBytes);
    }

    // MISMO APPUSER

    // Encriptar mensaje con clave pública

//    public byte[] encryptMessage(String message, RSAPublicKey publicKey) throws Exception {
//        RSAEngine engine = new RSAEngine();
//        RSAKeyParameters publicKeyParameters = new RSAKeyParameters(false, publicKey.getModulus(), publicKey.getPublicExponent());
//        engine.init(true, publicKeyParameters);
//
//        byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
//        return engine.processBlock(messageBytes, 0, messageBytes.length);
//    }
//
//    // Conversiones
//    public PublicKey stringToPublicKey(String publicKeyStr) throws GeneralSecurityException {
//        byte[] keyBytes = Base64.getDecoder().decode(publicKeyStr);
//        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
//        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
//        return keyFactory.generatePublic(keySpec);
//    }

    //

    public PrivateKey stringToPrivateKey(String privateKeyStr) throws GeneralSecurityException {
        byte[] keyBytes = Base64.getDecoder().decode(privateKeyStr);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(keySpec);
    }

    public static String publicKeyToString(PublicKey publicKey) {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    public static String privateKeyToString(PrivateKey privateKey) {
        return Base64.getEncoder().encodeToString(privateKey.getEncoded());
    }
}
