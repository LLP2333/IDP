package com.qvqw.idp.monitor.internal;

import com.qvqw.idp.common.exception.BusinessException;
import com.qvqw.idp.monitor.OperationLog;
import com.qvqw.idp.monitor.model.resp.LogDetailResp;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogServiceImplTest {

    @Mock
    private OperationLogRepository repository;

    @InjectMocks
    private LogServiceImpl service;

    @Test
    void recordLoginShouldPersistLoginLog() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
        request.addHeader("X-Real-IP", "192.168.1.8");
        request.addHeader("User-Agent", "Mozilla/5.0 Firefox/120.0 Windows");

        service.recordLogin("admin", false, "密码错误", request);

        ArgumentCaptor<OperationLog> captor = ArgumentCaptor.forClass(OperationLog.class);
        verify(repository).save(captor.capture());
        OperationLog log = captor.getValue();
        assertThat(log.getTraceId()).isNotBlank();
        assertThat(log.getModule()).isEqualTo("登录");
        assertThat(log.getDescription()).isEqualTo("登录失败");
        assertThat(log.getStatus()).isEqualTo(2);
        assertThat(log.getStatusCode()).isEqualTo(400);
        assertThat(log.getErrorMsg()).isEqualTo("密码错误");
        assertThat(log.getCreateUserString()).isEqualTo("admin");
        assertThat(log.getIp()).isEqualTo("192.168.1.8");
        assertThat(log.getBrowser()).isEqualTo("Firefox");
        assertThat(log.getOs()).isEqualTo("Windows");
        assertThat(log.getRequestMethod()).isEqualTo("POST");
        assertThat(log.getRequestUrl()).isEqualTo("/auth/login");
    }

    @Test
    void getShouldMapDetailFields() {
        OperationLog log = new OperationLog();
        log.setId(9L);
        log.setTraceId("trace-1");
        log.setDescription("修改 /system/user/1");
        log.setModule("用户管理");
        log.setTimeTaken(32L);
        log.setIp("127.0.0.1");
        log.setAddress("本机");
        log.setBrowser("Chrome");
        log.setOs("macOS");
        log.setStatus(1);
        log.setCreateUserString("管理员(admin)");
        log.setCreateTime(LocalDateTime.of(2026, 5, 24, 10, 0));
        log.setRequestUrl("/system/user/1");
        log.setRequestMethod("PUT");
        log.setRequestHeaders("{\"accept\":\"application/json\"}");
        log.setRequestBody("{\"nickname\":\"管理员\"}");
        log.setStatusCode(200);
        log.setResponseHeaders("{}");
        log.setResponseBody("{\"code\":0}");
        when(repository.findById(9L)).thenReturn(Optional.of(log));

        LogDetailResp resp = service.get(9L);

        assertThat(resp.getId()).isEqualTo("9");
        assertThat(resp.getTraceId()).isEqualTo("trace-1");
        assertThat(resp.getRequestUrl()).isEqualTo("/system/user/1");
        assertThat(resp.getRequestMethod()).isEqualTo("PUT");
        assertThat(resp.getStatusCode()).isEqualTo(200);
        assertThat(resp.getResponseBody()).isEqualTo("{\"code\":0}");
    }

    @Test
    void getMissingLogShouldThrowBusinessException() {
        when(repository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(404L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("日志不存在");
    }
}
