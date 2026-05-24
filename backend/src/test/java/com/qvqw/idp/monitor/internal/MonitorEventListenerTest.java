package com.qvqw.idp.monitor.internal;

import com.qvqw.idp.auth.LoginAuditEvent;
import com.qvqw.idp.auth.OnlineLoginEvent;
import com.qvqw.idp.auth.OnlineLogoutEvent;
import com.qvqw.idp.auth.OnlineTouchEvent;
import com.qvqw.idp.auth.OperationAuditEvent;
import com.qvqw.idp.monitor.LogService;
import com.qvqw.idp.monitor.OnlineUserService;
import com.qvqw.idp.monitor.OperationLog;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class MonitorEventListenerTest {

    private final OnlineUserService onlineUserService = mock(OnlineUserService.class);
    private final LogService logService = mock(LogService.class);
    private final MonitorEventListener listener = new MonitorEventListener(onlineUserService, logService);

    @Test
    void onlineEventsShouldDelegateToOnlineService() {
        listener.onOnlineLogin(new OnlineLoginEvent("token", "jti", 1L, "admin", "管理员", null));
        listener.onOnlineTouch(new OnlineTouchEvent("token"));
        listener.onOnlineLogout(new OnlineLogoutEvent("token"));

        verify(onlineUserService).recordLogin("token", "jti", 1L, "admin", "管理员", null);
        verify(onlineUserService).touch("token");
        verify(onlineUserService).remove("token");
    }

    @Test
    void auditEventsShouldDelegateToLogService() {
        listener.onLoginAudit(new LoginAuditEvent("admin", true, null, null));
        verify(logService).recordLogin("admin", true, null, null);

        LocalDateTime now = LocalDateTime.now();
        listener.onOperationAudit(new OperationAuditEvent(
                "trace", "修改 /system/user/1", "用户管理", 12L,
                "127.0.0.1", "本机", "Chrome", "macOS", 1, null,
                "admin", now, "/system/user/1", "PUT", "{}", "{}",
                200, "{}", "{\"code\":0}"));

        ArgumentCaptor<OperationLog> captor = ArgumentCaptor.forClass(OperationLog.class);
        verify(logService).record(captor.capture());
        OperationLog log = captor.getValue();
        assertThat(log.getTraceId()).isEqualTo("trace");
        assertThat(log.getModule()).isEqualTo("用户管理");
        assertThat(log.getRequestMethod()).isEqualTo("PUT");
        assertThat(log.getStatusCode()).isEqualTo(200);
    }
}
