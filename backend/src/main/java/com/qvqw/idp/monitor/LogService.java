package com.qvqw.idp.monitor;

import com.qvqw.idp.common.api.PageResp;
import com.qvqw.idp.monitor.model.query.LogQuery;
import com.qvqw.idp.monitor.model.resp.LogDetailResp;
import com.qvqw.idp.monitor.model.resp.LogResp;
import jakarta.servlet.http.HttpServletRequest;

/** 系统日志服务。 */
public interface LogService {

    /**
     * 记录登录审计日志。
     *
     * @param username 用户名
     * @param success  是否成功
     * @param errorMsg 失败原因
     * @param request  登录请求，可为空
     */
    void recordLogin(String username, boolean success, String errorMsg, HttpServletRequest request);

    /**
     * 记录系统操作日志。
     *
     * @param log 操作日志实体
     */
    void record(OperationLog log);

    /**
     * 分页查询系统日志。
     *
     * @param query 查询条件
     * @param page  页码，从 1 开始
     * @param size  每页数量
     * @return 日志分页数据
     */
    PageResp<LogResp> page(LogQuery query, int page, int size);

    /**
     * 查询系统日志详情。
     *
     * @param id 日志 ID
     * @return 日志详情
     */
    LogDetailResp get(Long id);
}
