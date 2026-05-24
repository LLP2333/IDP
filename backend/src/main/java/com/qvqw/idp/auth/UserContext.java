package com.qvqw.idp.auth;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * 当前登录用户上下文（跨模块对外暴露的轻量视图）。
 *
 * <p>由 {@link com.qvqw.idp.auth.internal.JwtAuthenticationFilter} 在每个请求开始时构造，
 * 写入到 {@link UserContextHolder} 中，业务层只读访问。</p>
 */
public class UserContext implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Long id;
    private final String username;
    private final String nickname;
    private final Set<String> roleCodes;

    public UserContext(Long id, String username, String nickname, Set<String> roleCodes) {
        this.id = id;
        this.username = username;
        this.nickname = nickname;
        this.roleCodes = roleCodes == null ? new HashSet<>() : new HashSet<>(roleCodes);
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
     * 判断当前用户是否拥有指定角色编码。
     *
     * @param code 角色编码（与 {@code idp_sys_role.code} 对齐）
     * @return 拥有该角色时返回 {@code true}
     */
    public boolean hasRole(String code) {
        return roleCodes.contains(code);
    }
}
