package com.qvqw.idp.menu.internal;

import com.qvqw.idp.common.exception.BusinessException;
import com.qvqw.idp.common.security.UserContext;
import com.qvqw.idp.common.security.UserContextHolder;
import com.qvqw.idp.menu.annotation.HasPermission;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Set;

/**
 * {@link HasPermission} 注解的 AOP 切面。
 *
 * <p>解析顺序：先方法上的注解 → 没有再读类上的注解；都没有时不拦截。匹配 {@code Mode.OR}（默认）
 * 时任一权限码命中即通过；{@code Mode.AND} 必须全部命中。</p>
 *
 * <p>未登录抛 {@code 401}，缺权限抛 {@code 403}；最终由
 * {@link com.qvqw.idp.common.exception.GlobalExceptionHandler} 转换为 {@code R} 形态 JSON。</p>
 */
@Aspect
@Component
public class MenuAspect {

    /**
     * 切入：方法或类上带 {@link HasPermission} 注解的所有 Bean 方法。
     */
    @Before("@annotation(com.qvqw.idp.menu.annotation.HasPermission) "
            + "|| @within(com.qvqw.idp.menu.annotation.HasPermission)")
    public void check(JoinPoint joinPoint) {
        HasPermission annotation = resolveAnnotation(joinPoint);
        if (annotation == null) {
            return;
        }
        UserContext ctx = UserContextHolder.get();
        if (ctx == null) {
            throw new BusinessException(401, "未登录或登录已过期");
        }
        Set<String> codes = ctx.getPermissionCodes();
        // admin 角色拥有全部权限
        if (ctx.hasRole("admin")) {
            return;
        }
        String[] required = annotation.value();
        if (required == null || required.length == 0) {
            return;
        }
        boolean granted;
        if (annotation.mode() == HasPermission.Mode.AND) {
            granted = true;
            for (String code : required) {
                if (!codes.contains(code)) {
                    granted = false;
                    break;
                }
            }
        } else {
            granted = false;
            for (String code : required) {
                if (codes.contains(code)) {
                    granted = true;
                    break;
                }
            }
        }
        if (!granted) {
            throw new BusinessException(403, "无访问权限");
        }
    }

    /**
     * 方法注解优先；缺失时回落到类注解。
     */
    private HasPermission resolveAnnotation(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        HasPermission methodAnn = AnnotationUtils.findAnnotation(method, HasPermission.class);
        if (methodAnn != null) {
            return methodAnn;
        }
        return AnnotationUtils.findAnnotation(method.getDeclaringClass(), HasPermission.class);
    }
}
