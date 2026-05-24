package com.qvqw.idp.auth;

import jakarta.servlet.http.HttpServletRequest;

/** 登录成功后的在线会话事件。 */
public record OnlineLoginEvent(String token, String jti, Long userId, String username, String nickname,
                               HttpServletRequest request) {
}
