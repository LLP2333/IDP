package com.qvqw.idp.user.internal;

import com.qvqw.idp.common.exception.BusinessException;
import com.qvqw.idp.option.OptionService;
import com.qvqw.idp.option.PasswordPolicy;
import com.qvqw.idp.user.UserPasswordHistory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 密码强度 / 重复校验工具。
 *
 * <p>消费 option 模块中维护的 8 项密码策略：</p>
 * <ul>
 *   <li>{@code PASSWORD_MIN_LENGTH}：最短长度；</li>
 *   <li>{@code PASSWORD_ALLOW_CONTAIN_USERNAME}：是否允许包含用户名（正反序）；</li>
 *   <li>{@code PASSWORD_REQUIRE_SYMBOLS}：是否必须包含特殊字符；</li>
 *   <li>{@code PASSWORD_REPETITION_TIMES}：禁止使用最近 N 次的历史密码。</li>
 * </ul>
 */
@Component
public class PasswordValidator {

    private final OptionService optionService;
    private final PasswordEncoder passwordEncoder;

    public PasswordValidator(OptionService optionService, PasswordEncoder passwordEncoder) {
        this.optionService = optionService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 校验新密码是否符合系统策略（不含历史重复校验）。
     *
     * @param plainPassword 待校验的明文新密码
     * @param username      用户名（用于 “是否允许包含用户名” 校验）
     * @throws BusinessException 校验不通过
     */
    public void validateStrength(String plainPassword, String username) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new BusinessException("密码不能为空");
        }
        PasswordPolicy.PASSWORD_MIN_LENGTH.validatePassword(plainPassword,
                optionService.getIntOrDefault(PasswordPolicy.PASSWORD_MIN_LENGTH.name(), 8), username);
        PasswordPolicy.PASSWORD_ALLOW_CONTAIN_USERNAME.validatePassword(plainPassword,
                optionService.getIntOrDefault(PasswordPolicy.PASSWORD_ALLOW_CONTAIN_USERNAME.name(), 1), username);
        PasswordPolicy.PASSWORD_REQUIRE_SYMBOLS.validatePassword(plainPassword,
                optionService.getIntOrDefault(PasswordPolicy.PASSWORD_REQUIRE_SYMBOLS.name(), 0), username);
    }

    /**
     * 校验是否命中历史密码（最近 N 条）。
     *
     * @param plainPassword 新密码（明文）
     * @param histories     历史密码记录（建议按时间倒序，自带过滤前 N 条）
     * @throws BusinessException 命中历史密码
     */
    public void validateHistory(String plainPassword, List<UserPasswordHistory> histories) {
        int times = optionService.getIntOrDefault(PasswordPolicy.PASSWORD_REPETITION_TIMES.name(), 3);
        if (histories == null || histories.isEmpty() || times <= 0) {
            return;
        }
        int limit = Math.min(times, histories.size());
        for (int i = 0; i < limit; i++) {
            if (passwordEncoder.matches(plainPassword, histories.get(i).getPasswordHash())) {
                throw new BusinessException("新密码不能与最近 %d 次历史密码相同".formatted(times));
            }
        }
    }

    /**
     * 一次性校验强度 + 历史。
     */
    public void validate(String plainPassword, String username, List<UserPasswordHistory> histories) {
        validateStrength(plainPassword, username);
        validateHistory(plainPassword, histories);
    }
}
