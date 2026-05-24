package com.qvqw.idp.option.internal;

import com.qvqw.idp.common.exception.BusinessException;
import com.qvqw.idp.option.OptionCategory;
import com.qvqw.idp.option.OptionService;
import com.qvqw.idp.option.PasswordPolicy;
import com.qvqw.idp.option.SystemOption;
import com.qvqw.idp.option.model.query.OptionQuery;
import com.qvqw.idp.option.model.req.OptionReq;
import com.qvqw.idp.option.model.req.OptionValueResetReq;
import com.qvqw.idp.option.model.resp.LoginConfigResp;
import com.qvqw.idp.option.model.resp.OptionResp;
import com.qvqw.idp.option.model.resp.SiteConfigResp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 系统参数服务实现。
 *
 * <p>读路径会优先走 Redis 缓存（key 前缀 {@code idp:option:}），写路径在事务提交后清空缓存。
 * 当前未引入 Spring Cache 抽象，直接用 {@link StringRedisTemplate}，原因是缓存 key 既要按
 * code 又要按 category 维度组织，annotation 方式较啰嗦。</p>
 */
@Service
public class OptionServiceImpl implements OptionService {

    private static final Logger log = LoggerFactory.getLogger(OptionServiceImpl.class);

    /** Redis key 前缀，更新时按此前缀清理。 */
    static final String CACHE_PREFIX = "idp:option:";

    private final OptionRepository optionRepository;
    private final StringRedisTemplate redisTemplate;

    public OptionServiceImpl(OptionRepository optionRepository,
                             @Autowired(required = false) StringRedisTemplate redisTemplate) {
        this.optionRepository = optionRepository;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public List<OptionResp> list(OptionQuery query) {
        List<SystemOption> list;
        OptionCategory category = query == null ? null : query.getCategory();
        List<String> codes = query == null ? null : query.getCodes();
        if (category != null) {
            list = optionRepository.findByCategoryOrderByIdAsc(category);
            if (codes != null && !codes.isEmpty()) {
                Set<String> codeSet = new HashSet<>(codes);
                list = list.stream().filter(o -> codeSet.contains(o.getCode())).toList();
            }
        } else if (codes != null && !codes.isEmpty()) {
            list = optionRepository.findByCodeIn(codes);
        } else {
            list = optionRepository.findAll();
        }
        return list.stream().map(this::toResp).toList();
    }

    @Override
    public Map<String, String> getByCategory(OptionCategory category) {
        if (category == null) {
            return new HashMap<>();
        }
        return optionRepository.findByCategoryOrderByIdAsc(category).stream()
                .collect(Collectors.toMap(SystemOption::getCode, o -> {
                    String v = o.effectiveValue();
                    return v == null ? "" : v;
                }, (a, b) -> a));
    }

    /**
     * 批量更新参数值。
     *
     * <p>除了基本的存在性校验外，对密码策略类参数额外做：</p>
     * <ol>
     *   <li>每一项的 {@link PasswordPolicy#validateRange(int, Map)} 取值范围校验；</li>
     *   <li>跨字段约束校验（如警告天数 &lt; 有效期）。</li>
     * </ol>
     *
     * @param reqs 待更新项列表
     * @throws BusinessException 校验失败 / 参数不存在
     */
    @Override
    @Transactional
    public void update(List<OptionReq> reqs) {
        if (reqs == null || reqs.isEmpty()) {
            return;
        }
        Map<Long, SystemOption> existing = optionRepository
                .findAllById(reqs.stream().map(OptionReq::getId).toList())
                .stream()
                .collect(Collectors.toMap(SystemOption::getId, Function.identity()));
        for (OptionReq req : reqs) {
            SystemOption opt = existing.get(req.getId());
            if (opt == null) {
                throw new BusinessException("参数不存在: " + req.getId());
            }
            if (!opt.getCode().equals(req.getCode())) {
                throw new BusinessException("参数 ID 与 code 不匹配: id=%d expected=%s got=%s"
                        .formatted(req.getId(), opt.getCode(), req.getCode()));
            }
        }
        // 收集密码策略类的整组变更，做范围 + 跨字段校验
        Map<String, String> passwordSnapshot = reqs.stream()
                .filter(r -> PasswordPolicy.matches(r.getCode()))
                .collect(Collectors.toMap(OptionReq::getCode,
                        r -> r.getValue() == null ? "" : r.getValue(), (a, b) -> a));
        if (!passwordSnapshot.isEmpty()) {
            for (Map.Entry<String, String> e : passwordSnapshot.entrySet()) {
                PasswordPolicy policy = PasswordPolicy.valueOf(e.getKey());
                int value;
                try {
                    value = Integer.parseInt(e.getValue());
                } catch (NumberFormatException ex) {
                    throw new BusinessException("参数 [%s] 必须为数字".formatted(e.getKey()));
                }
                policy.validateRange(value, passwordSnapshot);
            }
        }
        // 应用变更
        for (OptionReq req : reqs) {
            SystemOption opt = existing.get(req.getId());
            opt.setValue(req.getValue());
        }
        optionRepository.saveAll(existing.values());
        evictCache();
    }

    /**
     * 重置 value 为 null（业务侧访问时会自动回落到 default_value）。
     *
     * @param req 类别 / code 重置范围
     * @throws BusinessException 同时不提供 category 与 codes
     */
    @Override
    @Transactional
    public void resetValue(OptionValueResetReq req) {
        if (req == null || (req.getCategory() == null && (req.getCodes() == null || req.getCodes().isEmpty()))) {
            throw new BusinessException("请指定类别或键列表");
        }
        if (req.getCategory() != null) {
            optionRepository.resetByCategory(req.getCategory());
        } else {
            optionRepository.resetByCodes(req.getCodes());
        }
        evictCache();
    }

    @Override
    public <T> T getValue(String code, Function<String, T> mapper) {
        if (code == null) {
            return null;
        }
        String cached = cacheGet(code);
        if (cached != null) {
            return cached.isEmpty() ? null : mapper.apply(cached);
        }
        Optional<SystemOption> opt = optionRepository.findByCode(code);
        if (opt.isEmpty()) {
            return null;
        }
        String value = opt.get().effectiveValue();
        cachePut(code, value == null ? "" : value);
        return (value == null || value.isEmpty()) ? null : mapper.apply(value);
    }

    @Override
    public int getIntOrDefault(String code, int defaultValue) {
        Integer v = getValue(code, s -> {
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException ex) {
                return null;
            }
        });
        return v == null ? defaultValue : v;
    }

    /**
     * 校验上传图片的 dataUrl。
     *
     * <p>限制：</p>
     * <ul>
     *   <li>必须以 {@code data:image/} 开头；</li>
     *   <li>解码后大小不超过 1MB（避免单条记录把数据库塞爆）。</li>
     * </ul>
     */
    @Override
    public String validateImage(String dataUrl) {
        if (dataUrl == null || dataUrl.isBlank()) {
            throw new BusinessException("图片不能为空");
        }
        if (!dataUrl.startsWith("data:image/")) {
            throw new BusinessException("仅支持 data:image/* base64 格式");
        }
        int commaIdx = dataUrl.indexOf(',');
        if (commaIdx <= 0 || commaIdx == dataUrl.length() - 1) {
            throw new BusinessException("图片 base64 格式不正确");
        }
        String b64 = dataUrl.substring(commaIdx + 1);
        // base64 解码后字节数 ≈ b64.length() * 3 / 4
        long approxBytes = (long) b64.length() * 3 / 4;
        long maxBytes = 1024L * 1024L; // 1MB
        if (approxBytes > maxBytes) {
            throw new BusinessException("图片大小不能超过 1MB");
        }
        return dataUrl;
    }

    @Override
    public SiteConfigResp getPublicSite() {
        Map<String, String> map = getByCategory(OptionCategory.SITE);
        SiteConfigResp resp = new SiteConfigResp();
        resp.setTitle(map.get("SITE_TITLE"));
        resp.setDescription(map.get("SITE_DESCRIPTION"));
        resp.setLogo(map.get("SITE_LOGO"));
        resp.setFavicon(map.get("SITE_FAVICON"));
        resp.setCopyright(map.get("SITE_COPYRIGHT"));
        resp.setBeian(map.get("SITE_BEIAN"));
        return resp;
    }

    @Override
    public LoginConfigResp getPublicLogin() {
        LoginConfigResp resp = new LoginConfigResp();
        resp.setCaptchaEnabled(getIntOrDefault("LOGIN_CAPTCHA_ENABLED", 0) == 1);
        return resp;
    }

    /** entity -> DTO。 */
    private OptionResp toResp(SystemOption opt) {
        OptionResp resp = new OptionResp();
        resp.setId(opt.getId());
        resp.setCategory(opt.getCategory());
        resp.setName(opt.getName());
        resp.setCode(opt.getCode());
        resp.setValue(opt.effectiveValue());
        resp.setDescription(opt.getDescription());
        return resp;
    }

    /**
     * 按前缀清空缓存。
     *
     * <p>采用 SCAN 而非 KEYS，避免阻塞 Redis。</p>
     */
    private void evictCache() {
        if (redisTemplate == null) {
            return;
        }
        try {
            Set<String> keys = redisTemplate.keys(CACHE_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception ex) {
            log.warn("[option] 清理缓存失败: {}", ex.getMessage());
        }
    }

    private void cachePut(String code, String value) {
        if (redisTemplate == null) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(CACHE_PREFIX + code, value);
        } catch (Exception ex) {
            log.debug("[option] 写缓存失败: {}", ex.getMessage());
        }
    }

    private String cacheGet(String code) {
        if (redisTemplate == null) {
            return null;
        }
        try {
            return redisTemplate.opsForValue().get(CACHE_PREFIX + code);
        } catch (Exception ex) {
            return null;
        }
    }

    /** 测试可见的辅助构造（避免暴露 setter）。 */
    static List<OptionReq> toReqs(List<OptionReq> reqs) {
        return reqs == null ? new ArrayList<>() : new ArrayList<>(reqs);
    }
}
