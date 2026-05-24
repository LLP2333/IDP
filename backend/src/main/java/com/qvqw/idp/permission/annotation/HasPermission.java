package com.qvqw.idp.permission.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 鉴权注解。
 *
 * <p>用法：</p>
 * <pre>
 *   {@code @HasPermission("system:user:add")} 单权限
 *   {@code @HasPermission({"system:user:add", "system:user:update"})} 默认 OR
 *   {@code @HasPermission(value = {"system:user:add", "system:user:update"}, mode = HasPermission.Mode.AND)}
 * </pre>
 *
 * <p>未登录返回 401，已登录但缺权限返回 403；放在类上会对类下所有方法生效，方法上的注解会覆盖类上的。</p>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface HasPermission {

    /**
     * 需要校验的权限编码列表。
     */
    String[] value();

    /**
     * 多个权限的组合方式：默认 OR（任一满足即通过）。
     */
    Mode mode() default Mode.OR;

    enum Mode {
        OR, AND
    }
}
