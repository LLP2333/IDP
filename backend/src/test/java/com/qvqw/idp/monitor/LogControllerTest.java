package com.qvqw.idp.monitor;

import com.qvqw.idp.common.api.PageResp;
import com.qvqw.idp.monitor.model.query.LogQuery;
import com.qvqw.idp.monitor.model.resp.LogResp;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LogControllerTest {

    private final LogService logService = mock(LogService.class);
    private final LogController controller = new LogController(logService);

    @Test
    void pageShouldDelegateToService() {
        PageResp<LogResp> resp = new PageResp<>(List.of(), 0, 2, 20);
        LogQuery query = new LogQuery();
        when(logService.page(query, 2, 20)).thenReturn(resp);

        assertThat(controller.page(query, 2, 20).getData()).isSameAs(resp);
    }

    @Test
    void exportLoginShouldForceLoginModuleAndWriteCsv() throws Exception {
        LogResp row = new LogResp();
        row.setId("1");
        row.setCreateTime(LocalDateTime.of(2026, 5, 24, 10, 0));
        row.setModule("登录");
        row.setDescription("登录成功");
        row.setStatus(1);
        row.setCreateUserString("admin");
        row.setIp("127.0.0.1");
        row.setAddress("本机");
        row.setTimeTaken(0L);
        row.setBrowser("Chrome");
        row.setOs("macOS");
        when(logService.page(any(LogQuery.class), org.mockito.ArgumentMatchers.eq(1),
                org.mockito.ArgumentMatchers.eq(10_000)))
                .thenReturn(new PageResp<>(List.of(row), 1, 1, 10_000));
        MockHttpServletResponse response = new MockHttpServletResponse();

        LogQuery query = new LogQuery();
        controller.exportLogin(query, response);

        assertThat(query.getModule()).isEqualTo("登录");
        assertThat(response.getContentType()).isEqualTo("text/csv;charset=UTF-8");
        assertThat(response.getHeader("Content-Disposition")).contains("login-log.csv");
        assertThat(response.getContentAsString(StandardCharsets.UTF_8))
                .contains("id,createTime,module")
                .contains("\"登录成功\"");
        verify(logService).page(query, 1, 10_000);
    }
}
