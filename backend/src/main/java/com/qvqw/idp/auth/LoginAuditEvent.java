package com.qvqw.idp.auth;

import jakarta.servlet.http.HttpServletRequest;

/** 登录日志事件。 */
public record LoginAuditEvent(String username, boolean success, String errorMsg, HttpServletRequest request) {
}
