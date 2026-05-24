package com.qvqw.idp.common.security;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * 当前登录用户上下文（跨模块对外暴露的轻量视图）。
 *
 * <p>由 {@code auth} 模块的 JWT 过滤器在每个请求开始时构造，写入到
 * {@link UserContextHolder} 中，业务层只读访问。</p>
 *
 * <p>放在 {@code common.security} 包，避免 {@code permission → auth} 形成循环依赖
 * （auth 已依赖 user / role；如把上下文放在 auth，permission AOP 又要回头依赖 auth）。</p>
 */
public class UserContext implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Long id;
    private final String username;
    private final String nickname;
    private final Set<String> roleCodes;
    private final Set<String> permissionCodes;

    public UserContext(Long id, String username, String nickname, Set<String> roleCodes) {
        this(id, username, nickname, roleCodes, null);
    }

    public UserContext(Long id, String username, String nickname,
                       Set<String> roleCodes, Set<String> permissionCodes) {
        this.id = id;
        this.username = username;
        this.nickname = nickname;
        this.roleCodes = roleCodes == null ? new HashSet<>() : new HashSet<>(roleCodes);
        this.permissionCodes = permissionCodes == null ? new HashSet<>() : new HashSet<>(permissionCodes);
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getNickname() {
        return nickname;
    }

    public Set<String> getRoleCodes() {
        return roleCodes;
    }

    /**
     * 当前用户拥有的权限码集合（已聚合所有角色）。
     *
     * @return 权限码集合（不可为 {@code null}）
     */
    public Set<String> getPermissionCodes() {
        return permissionCodes;
    }

    /**
     * 判断当前用户是否拥有指定角色编码。
     *
     * @param code 角色编码（与 {@code idp_sys_role.code} 对齐）
     * @return 拥有该角色时返回 {@code true}
     */
    public boolean hasRole(String code) {
        return roleCodes.contains(code);
    }

    /**
     * 判断当前用户是否拥有指定权限码（admin 角色直通）。
     *
     * @param code 权限编码（如 {@code system:user:add}）
     * @return 拥有该权限时返回 {@code true}
     */
    public boolean hasPermission(String code) {
        return hasRole("admin") || permissionCodes.contains(code);
    }
}
