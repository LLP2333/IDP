package com.qvqw.idp.auth.internal;

import com.qvqw.idp.common.cache.AuthCacheEvictor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Redis 实现：基于 {@link JwtAuthenticationFilter#ROLE_CACHE_PREFIX} /
 * {@link JwtAuthenticationFilter#PERM_CACHE_PREFIX} 两个前缀清理缓存。
 */
@Component
public class RedisAuthCacheEvictor implements AuthCacheEvictor {

    private static final Logger log = LoggerFactory.getLogger(RedisAuthCacheEvictor.class);

    private final StringRedisTemplate redisTemplate;

    public RedisAuthCacheEvictor(@Autowired(required = false) StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void evictUser(Long userId) {
        if (redisTemplate == null || userId == null) {
            return;
        }
        try {
            redisTemplate.delete(List.of(
                    JwtAuthenticationFilter.ROLE_CACHE_PREFIX + userId,
                    JwtAuthenticationFilter.PERM_CACHE_PREFIX + userId));
        } catch (Exception ex) {
            log.debug("[auth] 清理用户 {} 鉴权缓存失败: {}", userId, ex.getMessage());
        }
    }

    @Override
    public void evictUsers(Iterable<Long> userIds) {
        if (userIds == null) {
            return;
        }
        for (Long uid : userIds) {
            evictUser(uid);
        }
    }

    @Override
    public void evictAll() {
        if (redisTemplate == null) {
            return;
        }
        try {
            List<String> keys = new ArrayList<>();
            Set<String> rs = redisTemplate.keys(JwtAuthenticationFilter.ROLE_CACHE_PREFIX + "*");
            Set<String> ps = redisTemplate.keys(JwtAuthenticationFilter.PERM_CACHE_PREFIX + "*");
            if (rs != null) {
                keys.addAll(rs);
            }
            if (ps != null) {
                keys.addAll(ps);
            }
            if (!keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception ex) {
            log.debug("[auth] 全量清理鉴权缓存失败: {}", ex.getMessage());
        }
    }
}
