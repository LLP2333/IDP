package com.qvqw.idp.permission.internal;

import com.qvqw.idp.common.exception.BusinessException;
import com.qvqw.idp.common.security.UserContext;
import com.qvqw.idp.common.security.UserContextHolder;
import com.qvqw.idp.permission.annotation.HasPermission;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 直接对 {@link PermissionAspect#check(JoinPoint)} 做单测，验证 AND / OR / admin 直通逻辑。
 */
class PermissionAspectTest {

    private final PermissionAspect aspect = new PermissionAspect();

    @AfterEach
    void clear() {
        UserContextHolder.clear();
    }

    @Test
    void unauthenticatedShouldThrow401() {
        JoinPoint joinPoint = jp("orMethod");
        assertThatThrownBy(() -> aspect.check(joinPoint))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("未登录");
    }

    @Test
    void adminShouldBypassRegardlessOfCodes() {
        UserContextHolder.set(new UserContext(1L, "admin", null, Set.of("admin"), Set.of()));
        assertThatCode(() -> aspect.check(jp("orMethod"))).doesNotThrowAnyException();
        assertThatCode(() -> aspect.check(jp("andMethod"))).doesNotThrowAnyException();
    }

    @Test
    void orModeAnyHitPasses() {
        UserContextHolder.set(new UserContext(1L, "u", null, Set.of("user"), Set.of("a")));
        assertThatCode(() -> aspect.check(jp("orMethod"))).doesNotThrowAnyException();
    }

    @Test
    void orModeAllMissShouldThrow403() {
        UserContextHolder.set(new UserContext(1L, "u", null, Set.of("user"), Set.of("x")));
        assertThatThrownBy(() -> aspect.check(jp("orMethod")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无访问权限");
    }

    @Test
    void andModeMissOneShouldThrow() {
        UserContextHolder.set(new UserContext(1L, "u", null, Set.of("user"), Set.of("a")));
        assertThatThrownBy(() -> aspect.check(jp("andMethod")))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void andModeAllHitPasses() {
        UserContextHolder.set(new UserContext(1L, "u", null, Set.of("user"), Set.of("a", "b")));
        assertThatCode(() -> aspect.check(jp("andMethod"))).doesNotThrowAnyException();
    }

    private static JoinPoint jp(String methodName) {
        try {
            Method method = Sample.class.getMethod(methodName);
            JoinPoint joinPoint = mock(JoinPoint.class);
            MethodSignature signature = mock(MethodSignature.class);
            when(signature.getMethod()).thenReturn(method);
            when(joinPoint.getSignature()).thenReturn((Signature) signature);
            return joinPoint;
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unused")
    static class Sample {

        @HasPermission({"a", "b"})
        public void orMethod() {
        }

        @HasPermission(value = {"a", "b"}, mode = HasPermission.Mode.AND)
        public void andMethod() {
        }
    }
}
