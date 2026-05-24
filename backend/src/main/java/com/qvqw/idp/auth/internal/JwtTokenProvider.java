package com.qvqw.idp.auth.internal;

import com.qvqw.idp.auth.AuthProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * JWT 签发与解析。
 */
@Component
public class JwtTokenProvider {

    private final AuthProperties properties;
    private final SecretKey key;

    public JwtTokenProvider(AuthProperties properties) {
        this.properties = properties;
        byte[] keyBytes = properties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    public IssuedToken issue(Long userId, String username) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(properties.getJwt().getExpires());
        String jti = UUID.randomUUID().toString();
        String token = Jwts.builder()
                .id(jti)
                .issuer(properties.getJwt().getIssuer())
                .subject(String.valueOf(userId))
                .claim("username", username)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key)
                .compact();
        return new IssuedToken(token, jti, userId, exp);
    }

    public ParsedToken parse(String token) {
        Jws<Claims> jws = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token);
        Claims c = jws.getPayload();
        Long userId = Long.valueOf(c.getSubject());
        String username = c.get("username", String.class);
        return new ParsedToken(c.getId(), userId, username);
    }

    public long getExpires() {
        return properties.getJwt().getExpires();
    }

    public record IssuedToken(String token, String jti, Long userId, Instant expiresAt) {
    }

    public record ParsedToken(String jti, Long userId, String username) {
    }
}
