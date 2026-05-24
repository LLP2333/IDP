package com.qvqw.idp.option;

import com.qvqw.idp.common.exception.BusinessException;

import java.util.Map;
import java.util.Objects;

/**
 * 密码策略枚举。
 *
 * <p>枚举值即 {@link SystemOption#code} 中以 {@code PASSWORD_} 开头的 8 个键；每个策略自带
 * 取值范围 ({@link #min} / {@link #max}) 与对密码本身的校验逻辑（{@link #validatePassword}）。</p>
 *
 * <p>参考自 {@code top.continew.admin.system.enums.PasswordPolicyEnum}，但去掉了对 SpringUtil
 * 的运行期反向依赖，跨字段联动改为显式传入策略上下文 Map。</p>
 */
public enum PasswordPolicy {

    /** 密码错误锁定阈值（0-10，0 表示禁用锁定）。 */
    PASSWORD_ERROR_LOCK_COUNT(0, 10) {
    },

    /** 账号锁定时长，单位分钟（1-1440）。 */
    PASSWORD_ERROR_LOCK_MINUTES(1, 1440) {
    },

    /** 密码有效期，单位天（0-999，0 表示永不过期）。 */
    PASSWORD_EXPIRATION_DAYS(0, 999) {
    },

    /** 密码到期提醒，单位天（0-998，0 表示不提醒；且必须小于密码有效期）。 */
    PASSWORD_EXPIRATION_WARNING_DAYS(0, 998) {
        @Override
        public void validateRange(int value, Map<String, String> policyMap) {
            super.validateRange(value, policyMap);
            if (policyMap == null) {
                return;
            }
            String expirationStr = policyMap.get(PASSWORD_EXPIRATION_DAYS.name());
            if (expirationStr == null) {
                return;
            }
            try {
                int expirationDays = Integer.parseInt(expirationStr);
                if (expirationDays > 0 && value >= expirationDays) {
                    throw new BusinessException("密码到期提醒时间应小于密码有效期");
                }
            } catch (NumberFormatException ignored) {
                // 让 PASSWORD_EXPIRATION_DAYS 自己抛
            }
        }
    },

    /** 历史密码重复校验次数（3-32）。 */
    PASSWORD_REPETITION_TIMES(3, 32) {
    },

    /** 密码最小长度（8-32）。 */
    PASSWORD_MIN_LENGTH(8, 32) {
        @Override
        public void validatePassword(String password, int value, String username) {
            if (password == null || password.length() < value) {
                throw new BusinessException("密码最小长度为 %d 个字符".formatted(value));
            }
        }
    },

    /** 是否允许密码包含正/反序的用户名（0=不允许，1=允许）。 */
    PASSWORD_ALLOW_CONTAIN_USERNAME(0, 1) {
        @Override
        public void validatePassword(String password, int value, String username) {
            if (value == 1 || password == null || username == null || username.isEmpty()) {
                return;
            }
            String lowerPwd = password.toLowerCase();
            String lowerUser = username.toLowerCase();
            String reversed = new StringBuilder(lowerUser).reverse().toString();
            if (lowerPwd.contains(lowerUser) || lowerPwd.contains(reversed)) {
                throw new BusinessException("密码不允许包含正反序用户名");
            }
        }
    },

    /** 密码是否必须包含特殊字符（0=不强制，1=强制）。 */
    PASSWORD_REQUIRE_SYMBOLS(0, 1) {
        @Override
        public void validatePassword(String password, int value, String username) {
            if (value != 1 || password == null) {
                return;
            }
            if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?~`].*")) {
                throw new BusinessException("密码必须包含特殊字符");
            }
        }
    };

    private final int min;
    private final int max;

    PasswordPolicy(int min, int max) {
        this.min = min;
        this.max = max;
    }

    public int getMin() {
        return min;
    }

    public int getMax() {
        return max;
    }

    /**
     * 校验取值是否在合法范围内。
     *
     * @param value     待校验的值
     * @param policyMap 当前正在变更的全量策略 Map（用于跨字段校验，可为 {@code null}）
     * @throws BusinessException 超出取值范围 / 违反跨字段约束
     */
    public void validateRange(int value, Map<String, String> policyMap) {
        if (value < min || value > max) {
            throw new BusinessException("%s 取值范围为 %d-%d".formatted(name(), min, max));
        }
    }

    /**
     * 校验密码本身是否满足该策略。默认实现不做任何校验，需要的策略覆盖即可。
     *
     * @param password 待校验的明文密码
     * @param value    当前策略值（如最小长度=12）
     * @param username 当前用户名（用于包含校验，可为 {@code null}）
     * @throws BusinessException 校验失败
     */
    public void validatePassword(String password, int value, String username) {
        // 默认无校验
    }

    /**
     * 类别（用于和 {@link OptionCategory} 关联）。
     */
    public static OptionCategory category() {
        return OptionCategory.PASSWORD;
    }

    /**
     * 检查给定 code 是否属于本枚举（即是否是密码类配置）。
     *
     * @param code SystemOption 的 code
     * @return 属于 PASSWORD 类别时返回 {@code true}
     */
    public static boolean matches(String code) {
        if (code == null) {
            return false;
        }
        for (PasswordPolicy p : values()) {
            if (Objects.equals(p.name(), code)) {
                return true;
            }
        }
        return false;
    }
}
