package com.qvqw.idp.monitor.internal;

import com.qvqw.idp.auth.LoginAuditEvent;
import com.qvqw.idp.auth.OnlineLoginEvent;
import com.qvqw.idp.auth.OnlineLogoutEvent;
import com.qvqw.idp.auth.OnlineTouchEvent;
import com.qvqw.idp.auth.OperationAuditEvent;
import com.qvqw.idp.monitor.LogService;
import com.qvqw.idp.monitor.OnlineUserService;
import com.qvqw.idp.monitor.OperationLog;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** 监听认证模块事件并写入监控数据。 */
@Component
public class MonitorEventListener {

    private final OnlineUserService onlineUserService;
    private final LogService logService;

    public MonitorEventListener(OnlineUserService onlineUserService, LogService logService) {
        this.onlineUserService = onlineUserService;
        this.logService = logService;
    }

    @EventListener
    public void onOnlineLogin(OnlineLoginEvent event) {
        onlineUserService.recordLogin(event.token(), event.jti(), event.userId(), event.username(),
                event.nickname(), event.request());
    }

    @EventListener
    public void onOnlineTouch(OnlineTouchEvent event) {
        onlineUserService.touch(event.token());
    }

    @EventListener
    public void onOnlineLogout(OnlineLogoutEvent event) {
        onlineUserService.remove(event.token());
    }

    @EventListener
    public void onLoginAudit(LoginAuditEvent event) {
        logService.recordLogin(event.username(), event.success(), event.errorMsg(), event.request());
    }

    @EventListener
    public void onOperationAudit(OperationAuditEvent event) {
        OperationLog log = new OperationLog();
        log.setTraceId(event.traceId());
        log.setDescription(event.description());
        log.setModule(event.module());
        log.setTimeTaken(event.timeTaken());
        log.setIp(event.ip());
        log.setAddress(event.address());
        log.setBrowser(event.browser());
        log.setOs(event.os());
        log.setStatus(event.status());
        log.setErrorMsg(event.errorMsg());
        log.setCreateUserString(event.createUserString());
        log.setCreateTime(event.createTime());
        log.setRequestUrl(event.requestUrl());
        log.setRequestMethod(event.requestMethod());
        log.setRequestHeaders(event.requestHeaders());
        log.setRequestBody(event.requestBody());
        log.setStatusCode(event.statusCode());
        log.setResponseHeaders(event.responseHeaders());
        log.setResponseBody(event.responseBody());
        logService.record(log);
    }
}
