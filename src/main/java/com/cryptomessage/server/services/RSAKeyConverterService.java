package com.cryptomessage.server.services;

import org.springframework.stereotype.Service;

import java.security.*;
import java.security.spec.*;
import java.util.Base64;

@Service
public class RSAKeyConverterService {

    private static final String PROVIDER = "BC";
    private static final String ALGORITHM = "RSA";

    /* ================= PUBLIC KEY ================= */

    public String publicKeyToString(PublicKey publicKey) {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    public PublicKey stringToPublicKey(String base64Key)
            throws GeneralSecurityException {

        byte[] decoded = Base64.getDecoder().decode(base64Key);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);

        return KeyFactory.getInstance(ALGORITHM, PROVIDER)
                .generatePublic(spec);
    }

    /* ================= PRIVATE KEY ================= */

    public String privateKeyToString(PrivateKey privateKey) {
        return Base64.getEncoder().encodeToString(privateKey.getEncoded());
    }

    public PrivateKey stringToPrivateKey(String base64Key)
            throws GeneralSecurityException {

        byte[] decoded = Base64.getDecoder().decode(base64Key);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);

        return KeyFactory.getInstance(ALGORITHM, PROVIDER)
                .generatePrivate(spec);
    }
}
