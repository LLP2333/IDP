package com.qvqw.idp.option.internal;

import com.qvqw.idp.common.exception.BusinessException;
import com.qvqw.idp.common.security.UserContext;
import com.qvqw.idp.common.security.UserContextHolder;
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
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
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

    /**
     * {@link OptionCategory} → 查询所需的权限码映射。
     *
     * <p>用于在 service 层做 fine-grained 二次鉴权：避免 Controller 上的粗粒度 OR 鉴权
     * 让 “只有 LOGIN 查询权” 的用户能跨类别看到 SITE / PASSWORD 参数。</p>
     */
    private static final Map<OptionCategory, String> READ_PERM;

    /**
     * {@link OptionCategory} → 修改所需的权限码映射。
     *
     * <p>用于 service 层 fine-grained 二次鉴权，防止跨类别越权改参数。</p>
     */
    private static final Map<OptionCategory, String> WRITE_PERM;

    static {
        READ_PERM = new EnumMap<>(OptionCategory.class);
        READ_PERM.put(OptionCategory.SITE, "system:siteConfig:get");
        READ_PERM.put(OptionCategory.PASSWORD, "system:securityConfig:get");
        READ_PERM.put(OptionCategory.LOGIN, "system:loginConfig:get");
        WRITE_PERM = new EnumMap<>(OptionCategory.class);
        WRITE_PERM.put(OptionCategory.SITE, "system:siteConfig:update");
        WRITE_PERM.put(OptionCategory.PASSWORD, "system:securityConfig:update");
        WRITE_PERM.put(OptionCategory.LOGIN, "system:loginConfig:update");
    }

    private final OptionRepository optionRepository;
    private final StringRedisTemplate redisTemplate;

    public OptionServiceImpl(OptionRepository optionRepository,
                             @Autowired(required = false) StringRedisTemplate redisTemplate) {
        this.optionRepository = optionRepository;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 列表查询参数。
     *
     * <p>鉴权策略（Controller 层粗粒度 OR 之外的 fine-grained 控制）：</p>
     * <ul>
     *   <li>显式指定 {@code category}：缺对应 {@code :get} 权限直接抛 {@code 403}；</li>
     *   <li>未指定类别（全量 / 按 codes）：返回值按 category 逐条过滤，只回吐用户有权读的项。</li>
     * </ul>
     */
    @Override
    public List<OptionResp> list(OptionQuery query) {
        UserContext ctx = UserContextHolder.get();
        List<SystemOption> list;
        OptionCategory category = query == null ? null : query.getCategory();
        List<String> codes = query == null ? null : query.getCodes();
        if (category != null) {
            requireAccess(ctx, category, READ_PERM, "查询");
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
        return list.stream()
                .filter(o -> canAccess(ctx, o.getCategory(), READ_PERM))
                .map(this::toResp)
                .toList();
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
     * <p>鉴权策略：除 Controller 层粗粒度 OR 校验外，service 内对每一条参数按其
     * {@link OptionCategory} 二次校验对应的 {@code :update} 权限码；
     * 即使请求里混杂多个类别，任一项越权也会整体 {@code 403} 回滚。</p>
     *
     * @param reqs 待更新项列表
     * @throws BusinessException 校验失败 / 参数不存在 / 类别越权
     */
    @Override
    @Transactional
    public void update(List<OptionReq> reqs) {
        if (reqs == null || reqs.isEmpty()) {
            return;
        }
        UserContext ctx = UserContextHolder.get();
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
            requireAccess(ctx, opt.getCategory(), WRITE_PERM, "修改");
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
     * <p>鉴权与 {@link #update(List)} 同源：按 category 校验对应的 {@code :update} 权限。
     * 按 codes 重置时，先反查每个 code 所属的类别，逐类别校验权限码，命中越权立即整体 {@code 403}。</p>
     *
     * @param req 类别 / code 重置范围
     * @throws BusinessException 同时不提供 category 与 codes / 类别越权
     */
    @Override
    @Transactional
    public void resetValue(OptionValueResetReq req) {
        if (req == null || (req.getCategory() == null && (req.getCodes() == null || req.getCodes().isEmpty()))) {
            throw new BusinessException("请指定类别或键列表");
        }
        UserContext ctx = UserContextHolder.get();
        if (req.getCategory() != null) {
            requireAccess(ctx, req.getCategory(), WRITE_PERM, "重置");
            optionRepository.resetByCategory(req.getCategory());
        } else {
            // 反查每个 code 所属类别（不存在的 code 直接抛 400，避免静默忽略）
            List<SystemOption> hits = optionRepository.findByCodeIn(req.getCodes());
            if (hits.size() != req.getCodes().size()) {
                Set<String> hitCodes = hits.stream()
                        .map(SystemOption::getCode)
                        .collect(Collectors.toSet());
                String missing = req.getCodes().stream()
                        .filter(c -> !hitCodes.contains(c))
                        .collect(Collectors.joining(","));
                throw new BusinessException("参数不存在: " + missing);
            }
            Set<OptionCategory> categories = hits.stream()
                    .map(SystemOption::getCategory)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            for (OptionCategory cat : categories) {
                requireAccess(ctx, cat, WRITE_PERM, "重置");
            }
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

    /**
     * 判断当前用户能否访问指定类别。
     *
     * <p>策略：</p>
     * <ul>
     *   <li>{@link UserContext} 为空（如内部服务调用、Seeder、定时任务）→ 直通；</li>
     *   <li>{@code admin} 角色 → 直通（与 {@link com.qvqw.idp.menu.internal.MenuAspect} 行为一致）；</li>
     *   <li>否则要求 {@code permMap.get(category)} 在权限码集合中。</li>
     * </ul>
     *
     * @param ctx      当前用户上下文（{@code null} 表示内部调用）
     * @param category 目标参数类别
     * @param permMap  类别 → 权限码映射（{@link #READ_PERM} 或 {@link #WRITE_PERM}）
     * @return 有权限返回 {@code true}
     */
    private static boolean canAccess(UserContext ctx, OptionCategory category,
                                     Map<OptionCategory, String> permMap) {
        if (ctx == null) {
            return true;
        }
        if (ctx.hasRole("admin")) {
            return true;
        }
        String code = permMap.get(category);
        return code != null && ctx.getPermissionCodes().contains(code);
    }

    /**
     * 没有权限就抛 {@code 403}。
     *
     * @param ctx       当前用户上下文
     * @param category  目标参数类别
     * @param permMap   类别 → 权限码映射
     * @param operation 操作动词（“查询” / “修改” / “重置”），用于错误提示
     * @throws BusinessException {@code 403} 当用户缺少对应权限
     */
    private static void requireAccess(UserContext ctx, OptionCategory category,
                                      Map<OptionCategory, String> permMap, String operation) {
        if (!canAccess(ctx, category, permMap)) {
            throw new BusinessException(403,
                    "无权限" + operation + categoryLabel(category) + "类参数");
        }
    }

    /** {@link OptionCategory} 的中文标签（仅用于错误提示）。 */
    private static String categoryLabel(OptionCategory category) {
        return switch (category) {
            case SITE -> "网站配置";
            case PASSWORD -> "安全配置";
            case LOGIN -> "登录配置";
        };
    }

    /** 测试可见的辅助构造（避免暴露 setter）。 */
    static List<OptionReq> toReqs(List<OptionReq> reqs) {
        return reqs == null ? new ArrayList<>() : new ArrayList<>(reqs);
    }
}
