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
 *
 * <p>使用 HS256 对称签名，secret 来自 {@link AuthProperties#getJwt()}。每个 token 都带有唯一 jti，
 * 与 Redis 中的会话记录配合实现服务端可注销的 JWT。</p>
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

    /**
     * 签发一枚 JWT。
     *
     * @param userId   用户 ID，作为 subject
     * @param username 用户名，放入自定义 claim
     * @return 签发结果（token、jti、userId、过期时间）
     */
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

    /**
     * 校验签名并解析 token。
     *
     * @param token 来自请求头的原始 JWT
     * @return 解析结果（jti、userId、username）
     * @throws io.jsonwebtoken.JwtException token 非法 / 过期时抛出
     */
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

    /**
     * 当前配置下 JWT 的有效期（秒）。
     *
     * @return 有效期
     */
    public long getExpires() {
        return properties.getJwt().getExpires();
    }

    /**
     * 签发结果。
     *
     * @param token     签名后的 JWT 字符串
     * @param jti       唯一标识，与 Redis 中的 token 记录关联
     * @param userId    所属用户 ID
     * @param expiresAt 过期时间
     */
    public record IssuedToken(String token, String jti, Long userId, Instant expiresAt) {
    }

    /**
     * 解析结果。
     *
     * @param jti      JWT ID
     * @param userId   subject 中的用户 ID
     * @param username 自定义 claim 中的用户名
     */
    public record ParsedToken(String jti, Long userId, String username) {
    }
}
