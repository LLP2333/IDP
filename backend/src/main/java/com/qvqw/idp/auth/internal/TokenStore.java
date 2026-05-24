package com.qvqw.idp.auth.internal;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 登录令牌 Redis 存储。
 *
 * <p>Key 格式：{@code idp:auth:token:{jti} -> userId}，TTL 与 JWT 过期一致，用于实现注销与续期。</p>
 */
@Component
public class TokenStore {

    private static final String KEY_PREFIX = "idp:auth:token:";

    private final StringRedisTemplate redis;

    public TokenStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void put(String jti, Long userId, long expiresSeconds) {
        redis.opsForValue().set(KEY_PREFIX + jti, String.valueOf(userId), Duration.ofSeconds(expiresSeconds));
    }

    public Long get(String jti) {
        String value = redis.opsForValue().get(KEY_PREFIX + jti);
        return value == null ? null : Long.valueOf(value);
    }

    public boolean exists(String jti) {
        Boolean has = redis.hasKey(KEY_PREFIX + jti);
        return Boolean.TRUE.equals(has);
    }

    public void remove(String jti) {
        redis.delete(KEY_PREFIX + jti);
    }
}
