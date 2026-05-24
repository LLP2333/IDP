package com.qvqw.idp.monitor;

import com.qvqw.idp.common.api.PageResp;
import com.qvqw.idp.common.api.R;
import com.qvqw.idp.menu.annotation.HasPermission;
import com.qvqw.idp.monitor.model.query.LogQuery;
import com.qvqw.idp.monitor.model.resp.LogDetailResp;
import com.qvqw.idp.monitor.model.resp.LogResp;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** 系统日志 API。 */
@Tag(name = "系统日志", description = "登录日志与操作日志查询")
@RestController
@RequestMapping("/system/log")
@Validated
public class LogController {

    private final LogService logService;

    public LogController(LogService logService) {
        this.logService = logService;
    }

    /**
     * 分页查询系统日志。
     *
     * @param query 查询条件
     * @param page  页码，从 1 开始
     * @param size  每页数量
     * @return 系统日志分页数据
     */
    @Operation(summary = "日志分页")
    @HasPermission("monitor:log:list")
    @GetMapping
    public R<PageResp<LogResp>> page(LogQuery query,
                                     @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") int page,
                                     @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size) {
        return R.ok(logService.page(query, page, size));
    }

    /**
     * 查询系统日志详情。
     *
     * @param id 日志 ID
     * @return 系统日志详情
     */
    @Operation(summary = "日志详情")
    @HasPermission("monitor:log:get")
    @GetMapping("/{id}")
    public R<LogDetailResp> get(@Parameter(description = "日志 ID") @PathVariable Long id) {
        return R.ok(logService.get(id));
    }

    /**
     * 导出登录日志 CSV。
     *
     * @param query    查询条件
     * @param response HTTP 响应
     * @throws IOException 写出响应失败
     */
    @Operation(summary = "导出登录日志")
    @HasPermission("monitor:log:export")
    @GetMapping("/export/login")
    public void exportLogin(LogQuery query, HttpServletResponse response) throws IOException {
        query.setModule("登录");
        exportCsv(logService.page(query, 1, 10_000).getList(), "login-log.csv", response);
    }

    /**
     * 导出操作日志 CSV。
     *
     * @param query    查询条件
     * @param response HTTP 响应
     * @throws IOException 写出响应失败
     */
    @Operation(summary = "导出操作日志")
    @HasPermission("monitor:log:export")
    @GetMapping("/export/operation")
    public void exportOperation(LogQuery query, HttpServletResponse response) throws IOException {
        exportCsv(logService.page(query, 1, 10_000).getList(), "operation-log.csv", response);
    }

    private void exportCsv(Iterable<LogResp> rows, String filename, HttpServletResponse response) throws IOException {
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + filename);
        response.getWriter().println("id,createTime,module,description,status,createUserString,ip,address,timeTaken,browser,os,errorMsg");
        for (LogResp row : rows) {
            response.getWriter().printf("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
                    csv(row.getId()), csv(String.valueOf(row.getCreateTime())),
                    csv(row.getModule()), csv(row.getDescription()),
                    csv(row.getStatus() == null ? "" : String.valueOf(row.getStatus())),
                    csv(row.getCreateUserString()), csv(row.getIp()), csv(row.getAddress()),
                    csv(row.getTimeTaken() == null ? "" : String.valueOf(row.getTimeTaken())),
                    csv(row.getBrowser()), csv(row.getOs()), csv(row.getErrorMsg()));
        }
    }

    private String csv(String value) {
        if (value == null) {
            return "";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
