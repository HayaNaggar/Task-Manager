package com.example.taskmanager.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${app.jwt.secret:changeitchangethissecret000000000000}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms:86400000}")
    private long jwtExpirationMs;

    private Key key() {
        byte[] bytes;
        try {
            bytes = Decoders.BASE64.decode(jwtSecret);
            if (bytes.length >= 32) {
                return Keys.hmacShaKeyFor(bytes);
            }
        } catch (IllegalArgumentException | io.jsonwebtoken.security.InvalidKeyException ex) {
            // Fall back to using the raw secret bytes when the configured secret is not a valid
            // Base64 key or is too short for HS256 after Base64 decoding.
        }

        bytes = jwtSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            bytes = java.util.Arrays.copyOf(bytes, 32);
        }
        return Keys.hmacShaKeyFor(bytes);
    }

    public String generateToken(String username, String role) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .setSubject(username)
                .claim("role", role)
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parserBuilder().setSigningKey(key()).build().parseClaimsJws(token).getBody();
    }

}
