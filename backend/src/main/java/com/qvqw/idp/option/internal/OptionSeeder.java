package com.qvqw.idp.option.internal;

import com.qvqw.idp.option.OptionCategory;
import com.qvqw.idp.option.SystemOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 系统参数默认值灌入。
 *
 * <p>启动时检查 15 条预置参数（SITE 6 + PASSWORD 8 + LOGIN 1）是否已存在；缺哪条补哪条，
 * 已有的不动，保证幂等，同时不覆盖管理员人工改过的值。</p>
 *
 * <p>参考自 continew-admin 的 {@code sys_option} 初始数据。</p>
 */
@Component
@Order(30)
public class OptionSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(OptionSeeder.class);

    private final OptionRepository optionRepository;

    public OptionSeeder(OptionRepository optionRepository) {
        this.optionRepository = optionRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        List<SystemOption> all = defaultOptions();
        int created = 0;
        int refreshed = 0;
        for (SystemOption o : all) {
            SystemOption existing = optionRepository.findByCode(o.getCode()).orElse(null);
            if (existing == null) {
                optionRepository.save(o);
                created++;
                continue;
            }
            // 既有记录：仅在 defaultValue 漂移时回写，永远不动 value（保留管理员手工修改）。
            if (!java.util.Objects.equals(existing.getDefaultValue(), o.getDefaultValue())) {
                existing.setDefaultValue(o.getDefaultValue());
                optionRepository.save(existing);
                refreshed++;
            }
        }
        if (created > 0) {
            log.info("[初始化] 已新增 {} 条系统参数默认值", created);
        }
        if (refreshed > 0) {
            log.info("[初始化] 已刷新 {} 条系统参数的 defaultValue", refreshed);
        }
    }

    /**
     * 返回内置的 15 条默认参数。
     *
     * @return 列表（按 SITE → PASSWORD → LOGIN 排列）
     */
    private List<SystemOption> defaultOptions() {
        List<SystemOption> list = new ArrayList<>(15);
        // SITE
        list.add(build(OptionCategory.SITE, "系统名称", "SITE_TITLE", "IDP 后台管理系统", "显示在浏览器标题栏与登录页的系统名称"));
        list.add(build(OptionCategory.SITE, "系统描述", "SITE_DESCRIPTION", "通用企业级后台管理系统", "用于 SEO 的网站元描述"));
        list.add(build(OptionCategory.SITE, "版权声明", "SITE_COPYRIGHT", "Copyright © IDP", "显示在页面底部的版权声明文本"));
        list.add(build(OptionCategory.SITE, "备案号", "SITE_BEIAN", null, "工信部 ICP 备案编号"));
        list.add(build(OptionCategory.SITE, "系统图标", "SITE_FAVICON", "/logo.png", "浏览器标签页显示的网站图标（建议 .ico / .png）"));
        list.add(build(OptionCategory.SITE, "系统 LOGO", "SITE_LOGO", "/logo.png", "登录页和系统导航栏显示的 Logo（建议 .svg / .png）"));
        // PASSWORD
        list.add(build(OptionCategory.PASSWORD, "密码错误锁定阈值", "PASSWORD_ERROR_LOCK_COUNT", "5", "连续登录失败次数（0-10，0=禁用锁定）"));
        list.add(build(OptionCategory.PASSWORD, "账号锁定时长（分钟）", "PASSWORD_ERROR_LOCK_MINUTES", "5", "账号锁定后自动解锁的时间（1-1440）"));
        list.add(build(OptionCategory.PASSWORD, "密码有效期（天）", "PASSWORD_EXPIRATION_DAYS", "0", "密码强制修改周期（0-999，0=永不过期）"));
        list.add(build(OptionCategory.PASSWORD, "密码到期提醒（天）", "PASSWORD_EXPIRATION_WARNING_DAYS", "0", "密码过期前的提前提醒天数（0=不提醒）"));
        list.add(build(OptionCategory.PASSWORD, "历史密码重复校验次数", "PASSWORD_REPETITION_TIMES", "3", "禁止使用最近 N 次的历史密码（3-32）"));
        list.add(build(OptionCategory.PASSWORD, "密码最小长度", "PASSWORD_MIN_LENGTH", "8", "密码最小字符长度要求（8-32）"));
        list.add(build(OptionCategory.PASSWORD, "是否允许密码包含用户名", "PASSWORD_ALLOW_CONTAIN_USERNAME", "1", "1=允许，0=禁止包含正反序用户名"));
        list.add(build(OptionCategory.PASSWORD, "密码是否必须包含特殊字符", "PASSWORD_REQUIRE_SYMBOLS", "0", "1=必须包含，0=不要求"));
        // LOGIN
        list.add(build(OptionCategory.LOGIN, "是否启用验证码", "LOGIN_CAPTCHA_ENABLED", "0",
                "1=启用登录验证码，0=关闭（默认关闭，管理员可在系统配置 - 登录配置中开启）"));
        return list;
    }

    /** 构造一条默认参数（{@code value} 留空，业务侧自动回落到 {@code defaultValue}）。 */
    private static SystemOption build(OptionCategory category, String name, String code,
                                      String defaultValue, String description) {
        SystemOption o = new SystemOption();
        o.setCategory(category);
        o.setName(name);
        o.setCode(code);
        o.setValue(null);
        o.setDefaultValue(defaultValue);
        o.setDescription(description);
        return o;
    }
}
