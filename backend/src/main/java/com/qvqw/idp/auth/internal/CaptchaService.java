package com.qvqw.idp.auth.internal;

import com.qvqw.idp.auth.model.resp.CaptchaResp;
import com.qvqw.idp.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录验证码服务。
 *
 * <p>逻辑：</p>
 * <ol>
 *   <li>生成 4 位字母 + 数字的随机字符串 + 唯一 UUID；</li>
 *   <li>把 {@code <uuid, code>} 写入 Redis（缺省 TTL 2 分钟），key 前缀
 *       {@code idp:auth:captcha:}；</li>
 *   <li>渲染为 SVG（无需 AWT，纯文本），返回给前端用 {@code <img src="data:image/svg+xml;base64,...">}；</li>
 *   <li>登录成功 / 失败一次都强制 {@link #consume(String, String)} 销毁验证码，防止复用。</li>
 * </ol>
 *
 * <p>当 Redis 不可用时退化到本地 {@link ConcurrentHashMap}，仅用于测试或临时降级，生产环境务必保证
 * Redis 在线，否则多实例间会不一致。</p>
 */
@Service
public class CaptchaService {

    private static final Logger log = LoggerFactory.getLogger(CaptchaService.class);
    private static final String CACHE_PREFIX = "idp:auth:captcha:";
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Duration TTL = Duration.ofMinutes(2);
    private static final int LENGTH = 4;

    private final StringRedisTemplate redisTemplate;
    private final boolean enableTestStore;
    private final Map<String, String> fallbackStore = new ConcurrentHashMap<>();

    public CaptchaService(@Autowired(required = false) StringRedisTemplate redisTemplate,
                          @Value("${idp.captcha.fallback-store:true}") boolean enableTestStore) {
        this.redisTemplate = redisTemplate;
        this.enableTestStore = enableTestStore;
    }

    /**
     * 生成一张验证码。
     *
     * @return 含 {@code captchaId} 与 base64 SVG 图片的响应
     */
    public CaptchaResp generate() {
        StringBuilder sb = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        String code = sb.toString();
        String uuid = UUID.randomUUID().toString().replace("-", "");
        store(uuid, code);
        String svg = renderSvg(code);
        String dataUrl = "data:image/svg+xml;base64,"
                + java.util.Base64.getEncoder().encodeToString(svg.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return new CaptchaResp(uuid, dataUrl);
    }

    /**
     * 消费验证码（成功匹配时清理；不匹配抛业务异常）。
     *
     * @param captchaId 生成时返回的 ID
     * @param input     用户输入
     * @throws BusinessException 不匹配 / 已过期
     */
    public void consume(String captchaId, String input) {
        if (captchaId == null || captchaId.isBlank() || input == null || input.isBlank()) {
            throw new BusinessException("请输入验证码");
        }
        String key = CACHE_PREFIX + captchaId;
        String expected = read(key);
        if (expected == null) {
            throw new BusinessException("验证码已过期，请刷新");
        }
        // 用完即删，无论成功失败都不允许复用
        remove(key);
        if (!expected.equalsIgnoreCase(input.trim())) {
            throw new BusinessException("验证码错误");
        }
    }

    /** 渲染 4 字符 SVG 验证码（带 3 条噪点线 + 字符旋转）。 */
    private static String renderSvg(String code) {
        int width = 120;
        int height = 40;
        StringBuilder svg = new StringBuilder(512);
        svg.append("<svg xmlns='http://www.w3.org/2000/svg' width='").append(width)
                .append("' height='").append(height).append("' viewBox='0 0 ").append(width).append(' ').append(height).append("'>");
        svg.append("<rect width='100%' height='100%' fill='#f5f5f5'/>");
        for (int i = 0; i < 3; i++) {
            int x1 = RANDOM.nextInt(width);
            int y1 = RANDOM.nextInt(height);
            int x2 = RANDOM.nextInt(width);
            int y2 = RANDOM.nextInt(height);
            svg.append("<line x1='").append(x1).append("' y1='").append(y1)
                    .append("' x2='").append(x2).append("' y2='").append(y2)
                    .append("' stroke='").append(randomColor()).append("' stroke-width='1'/>");
        }
        for (int i = 0; i < code.length(); i++) {
            int cx = 20 + i * 25;
            int cy = 28 + RANDOM.nextInt(6) - 3;
            int rotate = RANDOM.nextInt(40) - 20;
            svg.append("<text x='").append(cx).append("' y='").append(cy)
                    .append("' font-family='monospace' font-size='24' fill='").append(randomColor()).append("' ")
                    .append("transform='rotate(").append(rotate).append(' ').append(cx).append(',').append(cy).append(")'>")
                    .append(code.charAt(i)).append("</text>");
        }
        svg.append("</svg>");
        return svg.toString();
    }

    private static String randomColor() {
        int r = 30 + RANDOM.nextInt(120);
        int g = 30 + RANDOM.nextInt(120);
        int b = 30 + RANDOM.nextInt(120);
        return "rgb(%d,%d,%d)".formatted(r, g, b);
    }

    private void store(String uuid, String code) {
        String key = CACHE_PREFIX + uuid;
        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(key, code, TTL);
                return;
            } catch (Exception ex) {
                log.warn("[captcha] 写 Redis 失败，回落到本地存储: {}", ex.getMessage());
            }
        }
        if (enableTestStore) {
            fallbackStore.put(key, code);
        } else {
            throw new BusinessException("验证码服务不可用");
        }
    }

    private String read(String key) {
        if (redisTemplate != null) {
            try {
                String v = redisTemplate.opsForValue().get(key);
                if (v != null) {
                    return v;
                }
            } catch (Exception ex) {
                log.warn("[captcha] 读 Redis 失败，回落到本地存储: {}", ex.getMessage());
            }
        }
        return enableTestStore ? fallbackStore.get(key) : null;
    }

    private void remove(String key) {
        if (redisTemplate != null) {
            try {
                redisTemplate.delete(key);
            } catch (Exception ignored) {
                // 删除失败不阻塞业务
            }
        }
        if (enableTestStore) {
            fallbackStore.remove(key);
        }
    }

    /** 仅用于测试：清空本地降级存储。 */
    void clearForTest() {
        fallbackStore.clear();
    }

    /** 仅用于测试：返回当前本地存储的副本。 */
    Map<String, String> snapshotForTest() {
        return new HashMap<>(fallbackStore);
    }
}
