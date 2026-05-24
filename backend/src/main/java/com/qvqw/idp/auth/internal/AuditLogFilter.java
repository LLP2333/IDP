package com.qvqw.idp.auth.internal;

import com.qvqw.idp.auth.OperationAuditEvent;
import com.qvqw.idp.common.security.UserContext;
import com.qvqw.idp.common.security.UserContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

/** 记录系统操作日志的过滤器。 */
public class AuditLogFilter extends OncePerRequestFilter {

    private final ApplicationEventPublisher eventPublisher;

    public AuditLogFilter(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (shouldSkip(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        ContentCachingRequestWrapper req = new ContentCachingRequestWrapper(request, 8192);
        ContentCachingResponseWrapper resp = new ContentCachingResponseWrapper(response);
        long start = System.currentTimeMillis();
        Exception failure = null;
        try {
            filterChain.doFilter(req, resp);
        } catch (Exception ex) {
            failure = ex;
            throw ex;
        } finally {
            try {
                publishLog(req, resp, System.currentTimeMillis() - start, failure);
            } finally {
                resp.copyBodyToResponse();
            }
        }
    }

    private boolean shouldSkip(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/system/log")
                || uri.startsWith("/monitor/online")
                || uri.startsWith("/actuator")
                || uri.startsWith("/v3/api-docs")
                || uri.startsWith("/swagger-ui")
                || "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    private void publishLog(ContentCachingRequestWrapper request,
                            ContentCachingResponseWrapper response,
                            long timeTaken,
                            Exception failure) {
        String ua = request.getHeader("User-Agent");
        String ip = clientIp(request);
        UserContext ctx = UserContextHolder.get();
        String query = request.getQueryString();
        eventPublisher.publishEvent(new OperationAuditEvent(
                UUID.randomUUID().toString(),
                resolveDescription(request),
                resolveModule(request),
                timeTaken,
                ip,
                ip,
                browser(ua),
                os(ua),
                failure == null && response.getStatus() < 400 ? 1 : 2,
                failure == null ? null : trim(failure.getMessage(), 1000),
                ctx == null ? null : displayName(ctx),
                LocalDateTime.now(),
                query == null ? request.getRequestURI() : request.getRequestURI() + "?" + query,
                request.getMethod(),
                headers(request),
                body(request.getContentAsByteArray()),
                response.getStatus(),
                responseHeaders(response),
                body(response.getContentAsByteArray())));
    }

    private String displayName(UserContext ctx) {
        if (ctx.getNickname() != null && !ctx.getNickname().isBlank()) {
            return ctx.getNickname() + "(" + ctx.getUsername() + ")";
        }
        return ctx.getUsername();
    }

    private String resolveModule(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri.startsWith("/system/user")) return "用户管理";
        if (uri.startsWith("/system/role")) return "角色管理";
        if (uri.startsWith("/system/menu")) return "菜单管理";
        if (uri.startsWith("/system/dict")) return "字典管理";
        if (uri.startsWith("/system/notice")) return "通知公告";
        if (uri.startsWith("/system/option")) return "系统配置";
        if (uri.startsWith("/system/message")) return "消息中心";
        if (uri.startsWith("/auth")) return "认证";
        return "系统操作";
    }

    private String resolveDescription(HttpServletRequest request) {
        String action = switch (request.getMethod()) {
            case "GET" -> "查询";
            case "POST" -> "新增/提交";
            case "PUT" -> "修改";
            case "PATCH" -> "局部修改";
            case "DELETE" -> "删除";
            default -> request.getMethod();
        };
        return action + " " + request.getRequestURI();
    }

    private String headers(HttpServletRequest request) {
        StringBuilder sb = new StringBuilder("{");
        request.getHeaderNames().asIterator().forEachRemaining(name -> {
            if (sb.length() > 1) sb.append(',');
            sb.append('"').append(name).append("\":\"")
                    .append(trim(request.getHeader(name), 300).replace("\"", "\\\""))
                    .append('"');
        });
        return trim(sb.append('}').toString(), 8000);
    }

    private String responseHeaders(HttpServletResponse response) {
        StringBuilder sb = new StringBuilder("{");
        for (String name : response.getHeaderNames()) {
            if (sb.length() > 1) sb.append(',');
            sb.append('"').append(name).append("\":\"")
                    .append(trim(response.getHeader(name), 300).replace("\"", "\\\""))
                    .append('"');
        }
        return trim(sb.append('}').toString(), 8000);
    }

    private String body(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return null;
        return trim(new String(bytes, StandardCharsets.UTF_8), 8000);
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) return forwarded.split(",")[0].trim();
        String realIp = request.getHeader("X-Real-IP");
        return realIp == null || realIp.isBlank() ? request.getRemoteAddr() : realIp.trim();
    }

    private String browser(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) return null;
        String ua = userAgent.toLowerCase();
        if (ua.contains("edg/")) return "Edge";
        if (ua.contains("chrome/")) return "Chrome";
        if (ua.contains("firefox/")) return "Firefox";
        if (ua.contains("safari/")) return "Safari";
        return "未知";
    }

    private String os(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) return null;
        String ua = userAgent.toLowerCase();
        if (ua.contains("windows")) return "Windows";
        if (ua.contains("mac os")) return "macOS";
        if (ua.contains("iphone") || ua.contains("ipad")) return "iOS";
        if (ua.contains("android")) return "Android";
        if (ua.contains("linux")) return "Linux";
        return "未知";
    }

    private String trim(String value, int max) {
        if (value == null || value.length() <= max) return value;
        return value.substring(0, max);
    }
}
