package com.qvqw.idp.common.cache;

/**
 * 鉴权缓存清理接口（对外暴露给 role / user / permission 模块在写操作后调用）。
 *
 * <p>auth 模块在 Redis 中按用户缓存角色 / 权限码以加速 JWT 过滤器；任何会影响用户角色或权限的
 * 写操作都应在事务提交后调用这里的 {@code evict*} 方法，否则缓存最长 5 分钟才会过期。</p>
 *
 * <p>放在 {@code common.cache} 包而非 {@code auth} 包，是为了避免 {@code user → auth} 形成
 * 循环依赖（auth 模块本身已依赖 user / role）。</p>
 */
public interface AuthCacheEvictor {

    /** 清理指定用户的角色 / 权限缓存（{@code null} 时无操作）。 */
    void evictUser(Long userId);

    /** 按用户 ID 列表批量清理。 */
    void evictUsers(Iterable<Long> userIds);

    /** 全量清空（角色 / 权限批量变更时使用）。 */
    void evictAll();
}
