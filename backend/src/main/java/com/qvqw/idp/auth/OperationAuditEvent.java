package com.qvqw.idp.auth;

import java.time.LocalDateTime;

/** 系统操作日志事件。 */
public record OperationAuditEvent(
        String traceId,
        String description,
        String module,
        Long timeTaken,
        String ip,
        String address,
        String browser,
        String os,
        Integer status,
        String errorMsg,
        String createUserString,
        LocalDateTime createTime,
        String requestUrl,
        String requestMethod,
        String requestHeaders,
        String requestBody,
        Integer statusCode,
        String responseHeaders,
        String responseBody) {
}
