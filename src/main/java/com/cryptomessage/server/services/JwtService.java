package com.cryptomessage.server.services;

import com.cryptomessage.server.model.persistance.user.AppUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.function.Function;
//optional for custom claims


@Service
public class JwtService {

    private SecretKey secretKey;

    @Value("${jwt.secret-key}")
    private String jwtSecretKey;

    @Value("${jwt.expiration-ms}")
    private Long jwtExpirationMs;

    @PostConstruct
    public void init() {
        byte[] bytes = Base64.getDecoder().decode(
                jwtSecretKey.getBytes(
                        StandardCharsets.UTF_8
                ));
                this.secretKey = new SecretKeySpec(bytes, "HmacSHA256");
    }

//    public String generateToken(UserDetails userDetails) {
    public String generateToken(AppUser appUser, String passphrase) {
        return Jwts.builder()
//                .setClaims(new HashMap<>()) // This is optional to set custom claims
                .claim("passphrase", passphrase)
                .subject(appUser.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getSignInKey())
                .compact();
    }

    private SecretKey getSignInKey(){
        return secretKey;
    }

    public Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSignInKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            // Manejar excepciones de JWT (por ejemplo, token expirado o malformado)
            throw new IllegalArgumentException("Token inválido" + token);
        }
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractPassphrase(String token) {
        return extractClaim(token, claims -> claims.get("passphrase", String.class));
    }

    public Boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
}
