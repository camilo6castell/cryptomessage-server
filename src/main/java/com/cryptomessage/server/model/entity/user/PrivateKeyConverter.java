package com.cryptomessage.server.model.entity.user;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

@Converter
public class PrivateKeyConverter implements AttributeConverter<PrivateKey, String> {

    @Override
    public String convertToDatabaseColumn(PrivateKey privateKey) {
        if (privateKey == null) return null;
        return Base64.getEncoder().encodeToString(privateKey.getEncoded());
    }

    @Override
    public PrivateKey convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        try {
            byte[] keyBytes = Base64.getDecoder().decode(dbData);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePrivate(spec);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to convert String to PrivateKey", e);
        }
    }
}

