package com.qvqw.idp.file.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qvqw.idp.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 分片上传状态缓存：优先用 Redis，缺失时回落到内存 Map（测试场景）。
 *
 * <p>key 规范：{@code idp:file:mu:<uploadId>}，TTL 默认 24 小时。</p>
 */
@Component
class MultipartUploadStateCache {

    private static final Logger log = LoggerFactory.getLogger(MultipartUploadStateCache.class);
    private static final String PREFIX = "idp:file:mu:";
    private static final Duration TTL = Duration.ofHours(24);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, String> fallbackStore = new ConcurrentHashMap<>();

    MultipartUploadStateCache(@Autowired(required = false) StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 写入状态。
     */
    void save(MultipartUploadState state) {
        try {
            String json = objectMapper.writeValueAsString(state);
            String key = PREFIX + state.getUploadId();
            if (redisTemplate != null) {
                try {
                    redisTemplate.opsForValue().set(key, json, TTL);
                    return;
                } catch (Exception e) {
                    log.warn("Redis 不可用，分片状态回落到内存：{}", e.getMessage());
                }
            }
            fallbackStore.put(key, json);
        } catch (Exception e) {
            throw new BusinessException("保存分片上传状态失败: " + e.getMessage());
        }
    }

    /**
     * 读取状态，找不到返回 {@code null}。
     */
    MultipartUploadState get(String uploadId) {
        String key = PREFIX + uploadId;
        String json = null;
        if (redisTemplate != null) {
            try {
                json = redisTemplate.opsForValue().get(key);
            } catch (Exception e) {
                log.warn("读取 Redis 分片状态失败：{}", e.getMessage());
            }
        }
        if (json == null) {
            json = fallbackStore.get(key);
        }
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, MultipartUploadState.class);
        } catch (Exception e) {
            throw new BusinessException("反序列化分片状态失败: " + e.getMessage());
        }
    }

    /**
     * 删除状态。
     */
    void remove(String uploadId) {
        String key = PREFIX + uploadId;
        if (redisTemplate != null) {
            try {
                redisTemplate.delete(key);
            } catch (Exception e) {
                log.warn("删除 Redis 分片状态失败：{}", e.getMessage());
            }
        }
        fallbackStore.remove(key);
    }
}
