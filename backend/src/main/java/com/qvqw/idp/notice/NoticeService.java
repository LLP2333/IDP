package com.qvqw.idp.notice;

import com.qvqw.idp.common.api.PageResp;
import com.qvqw.idp.notice.model.query.NoticeQuery;
import com.qvqw.idp.notice.model.req.NoticeReq;
import com.qvqw.idp.notice.model.resp.DashboardNoticeResp;
import com.qvqw.idp.notice.model.resp.NoticeDetailResp;
import com.qvqw.idp.notice.model.resp.NoticeResp;

import java.util.List;

/**
 * 公告服务（对外暴露）。
 */
public interface NoticeService {

    /**
     * 分页查询公告（管理端）。
     *
     * @param query 查询条件
     * @param page  页码（从 1 开始）
     * @param size  每页大小
     * @return 分页结果
     */
    PageResp<NoticeResp> page(NoticeQuery query, int page, int size);

    /**
     * 查询公告详情。
     *
     * @param id 公告 ID
     * @return 公告详情
     */
    NoticeDetailResp get(Long id);

    /**
     * 新增公告（草稿 / 立即发布 / 定时发布由 isTiming + status 推导）。
     *
     * @param req 公告请求体
     * @return 新建公告 ID
     */
    Long create(NoticeReq req);

    /**
     * 修改公告。已发布的公告禁止修改通知范围 / 方式 / 定时设置。
     *
     * @param id  公告 ID
     * @param req 公告请求体
     */
    void update(Long id, NoticeReq req);

    /**
     * 批量删除公告，同时删除对应的 {@code notice_log}。
     *
     * @param ids 公告 ID 列表
     */
    void delete(List<Long> ids);

    /**
     * 立即发布公告（被定时任务调用）。
     *
     * @param id 公告 ID
     */
    void publishNow(Long id);

    /**
     * 当前用户的登录弹窗公告列表（未读且 method=POPUP）。
     *
     * @return 公告详情列表
     */
    List<NoticeDetailResp> listPopupForCurrentUser();

    /**
     * 当前用户阅读某条公告（写入 {@code notice_log}，幂等）。
     *
     * @param id 公告 ID
     */
    void read(Long id);

    /**
     * Dashboard 最新公告摘要（含 “对当前用户是否已读”）。
     *
     * @param limit 摘要条数（建议 5-10）
     * @return 摘要列表
     */
    List<DashboardNoticeResp> listDashboard(int limit);

    /**
     * 列出全部到点的待发布公告（{@code status=PENDING} 且 {@code publish_time <= now}）。
     *
     * @return 公告 ID 列表
     */
    List<Long> listPendingDueIds();
}
