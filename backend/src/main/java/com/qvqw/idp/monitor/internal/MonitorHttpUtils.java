package com.qvqw.idp.monitor.internal;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

/** 监控模块 HTTP 辅助方法。 */
final class MonitorHttpUtils {

    private MonitorHttpUtils() {
    }

    static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    static String browser(String userAgent) {
        if (!StringUtils.hasText(userAgent)) {
            return null;
        }
        String ua = userAgent.toLowerCase();
        if (ua.contains("edg/")) return "Edge";
        if (ua.contains("chrome/")) return "Chrome";
        if (ua.contains("firefox/")) return "Firefox";
        if (ua.contains("safari/")) return "Safari";
        return "未知";
    }

    static String os(String userAgent) {
        if (!StringUtils.hasText(userAgent)) {
            return null;
        }
        String ua = userAgent.toLowerCase();
        if (ua.contains("windows")) return "Windows";
        if (ua.contains("mac os")) return "macOS";
        if (ua.contains("iphone") || ua.contains("ipad")) return "iOS";
        if (ua.contains("android")) return "Android";
        if (ua.contains("linux")) return "Linux";
        return "未知";
    }

    static String trim(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }
}
