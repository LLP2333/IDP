package com.qvqw.idp.common.security;

/**
 * 当前线程上下文持有者。
 *
 * <p>由 auth 模块的 JWT 过滤器注入；业务层只读访问；过滤器在请求结束时调用
 * {@link #clear()} 释放，避免线程复用导致信息泄漏。</p>
 */
public final class UserContextHolder {

    private static final ThreadLocal<UserContext> HOLDER = new ThreadLocal<>();

    private UserContextHolder() {
    }

    /** 把上下文写入当前线程。 */
    public static void set(UserContext context) {
        HOLDER.set(context);
    }

    /** 读取当前线程绑定的用户上下文；未登录返回 {@code null}。 */
    public static UserContext get() {
        return HOLDER.get();
    }

    /** 当前登录用户 ID 的便捷取值。 */
    public static Long getUserId() {
        UserContext ctx = HOLDER.get();
        return ctx == null ? null : ctx.getId();
    }

    /** 当前登录用户名的便捷取值。 */
    public static String getUsername() {
        UserContext ctx = HOLDER.get();
        return ctx == null ? null : ctx.getUsername();
    }

    /** 清除当前线程的上下文，过滤器请求结束时必须调用。 */
    public static void clear() {
        HOLDER.remove();
    }
}
