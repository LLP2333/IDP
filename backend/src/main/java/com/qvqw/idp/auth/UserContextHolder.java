package com.qvqw.idp.auth;

/**
 * 当前线程上下文持有者。
 *
 * <p>由 {@code JwtAuthenticationFilter} 注入，业务层只读访问。</p>
 */
public final class UserContextHolder {

    private static final ThreadLocal<UserContext> HOLDER = new ThreadLocal<>();

    private UserContextHolder() {
    }

    public static void set(UserContext context) {
        HOLDER.set(context);
    }

    public static UserContext get() {
        return HOLDER.get();
    }

    public static Long getUserId() {
        UserContext ctx = HOLDER.get();
        return ctx == null ? null : ctx.getId();
    }

    public static String getUsername() {
        UserContext ctx = HOLDER.get();
        return ctx == null ? null : ctx.getUsername();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
