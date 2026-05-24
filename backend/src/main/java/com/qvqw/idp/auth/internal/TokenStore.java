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

    /** Redis key 前缀，集中维护方便后续清理或迁移。 */
    private static final String KEY_PREFIX = "idp:auth:token:";

    private final StringRedisTemplate redis;

    public TokenStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /**
     * 写入一条会话记录。
     *
     * @param jti            JWT ID
     * @param userId         所属用户 ID
     * @param expiresSeconds TTL 秒数（应与 JWT 过期一致）
     */
    public void put(String jti, Long userId, long expiresSeconds) {
        redis.opsForValue().set(KEY_PREFIX + jti, String.valueOf(userId), Duration.ofSeconds(expiresSeconds));
    }

    /**
     * 读取 jti 对应的用户 ID。
     *
     * @param jti JWT ID
     * @return 用户 ID；不存在时为 {@code null}
     */
    public Long get(String jti) {
        String value = redis.opsForValue().get(KEY_PREFIX + jti);
        return value == null ? null : Long.valueOf(value);
    }

    /**
     * 判断 jti 对应的会话是否仍然有效。
     *
     * @param jti JWT ID
     * @return 有效返回 {@code true}
     */
    public boolean exists(String jti) {
        Boolean has = redis.hasKey(KEY_PREFIX + jti);
        return Boolean.TRUE.equals(has);
    }

    /**
     * 强制让指定 jti 立即失效（注销）。
     *
     * @param jti JWT ID
     */
    public void remove(String jti) {
        redis.delete(KEY_PREFIX + jti);
    }
}
