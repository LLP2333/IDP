package com.qvqw.idp.option;

import com.qvqw.idp.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link PasswordPolicy} 的 8 项策略各自的取值范围与密码校验逻辑。
 */
class PasswordPolicyTest {

    @Test
    void rangeShouldRejectOutOfBoundValue() {
        assertThatThrownBy(() -> PasswordPolicy.PASSWORD_MIN_LENGTH.validateRange(5, Map.of()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("取值范围");
    }

    @Test
    void warningDaysMustBeLessThanExpirationDays() {
        Map<String, String> snap = Map.of(
                PasswordPolicy.PASSWORD_EXPIRATION_DAYS.name(), "30",
                PasswordPolicy.PASSWORD_EXPIRATION_WARNING_DAYS.name(), "30");
        assertThatThrownBy(() -> PasswordPolicy.PASSWORD_EXPIRATION_WARNING_DAYS.validateRange(30, snap))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("应小于密码有效期");
    }

    @Test
    void minLengthValidationShouldFailWhenTooShort() {
        assertThatThrownBy(() -> PasswordPolicy.PASSWORD_MIN_LENGTH.validatePassword("abc", 8, "user"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("密码最小长度");
    }

    @Test
    void containUsernameValidation() {
        // value = 0 表示不允许包含
        assertThatThrownBy(() -> PasswordPolicy.PASSWORD_ALLOW_CONTAIN_USERNAME
                .validatePassword("Pass-zhangsan-1", 0, "zhangsan"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("正反序用户名");
        // 反序
        assertThatThrownBy(() -> PasswordPolicy.PASSWORD_ALLOW_CONTAIN_USERNAME
                .validatePassword("Pass-nasgnahz-1", 0, "zhangsan"))
                .isInstanceOf(BusinessException.class);
        // value = 1 直接放行
        PasswordPolicy.PASSWORD_ALLOW_CONTAIN_USERNAME
                .validatePassword("Pass-zhangsan-1", 1, "zhangsan");
    }

    @Test
    void requireSymbols() {
        assertThatThrownBy(() -> PasswordPolicy.PASSWORD_REQUIRE_SYMBOLS
                .validatePassword("abcd1234", 1, "user"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("特殊字符");
        PasswordPolicy.PASSWORD_REQUIRE_SYMBOLS.validatePassword("abcd1234!", 1, "user");
        // value = 0 直接放行
        PasswordPolicy.PASSWORD_REQUIRE_SYMBOLS.validatePassword("abcd1234", 0, "user");
    }

    @Test
    void matchesShouldDetectKnownCodes() {
        assertThat(PasswordPolicy.matches("PASSWORD_MIN_LENGTH")).isTrue();
        assertThat(PasswordPolicy.matches("SITE_TITLE")).isFalse();
    }
}
