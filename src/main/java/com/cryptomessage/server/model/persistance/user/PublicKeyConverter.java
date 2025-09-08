package com.cryptomessage.server.model.persistance.user;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Converter
public class PublicKeyConverter implements AttributeConverter<PublicKey, String> {

    @Override
    public String convertToDatabaseColumn(PublicKey publicKey) {
        if (publicKey == null) return null;
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    @Override
    public PublicKey convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        try {
            byte[] keyBytes = Base64.getDecoder().decode(dbData);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePublic(spec);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to convert String to PublicKey", e);
        }
    }
}

