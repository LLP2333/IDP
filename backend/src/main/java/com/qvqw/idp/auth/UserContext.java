package com.qvqw.idp.auth;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * 当前登录用户上下文（跨模块对外暴露的轻量视图）。
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

    public boolean hasRole(String code) {
        return roleCodes.contains(code);
    }
}
