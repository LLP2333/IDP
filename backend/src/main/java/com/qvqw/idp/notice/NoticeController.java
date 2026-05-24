package com.qvqw.idp.notice;

import com.qvqw.idp.common.api.PageResp;
import com.qvqw.idp.common.api.R;
import com.qvqw.idp.menu.annotation.HasPermission;
import com.qvqw.idp.notice.model.query.NoticeQuery;
import com.qvqw.idp.notice.model.req.NoticeReq;
import com.qvqw.idp.notice.model.resp.DashboardNoticeResp;
import com.qvqw.idp.notice.model.resp.NoticeDetailResp;
import com.qvqw.idp.notice.model.resp.NoticeResp;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 公告管理 API：管理端 CRUD + 普通用户阅读 / 弹窗 / Dashboard 摘要。
 *
 * <p>管理端接口需要 {@code system:notice:*} 权限；普通用户的阅读 / 弹窗 / Dashboard
 * 接口只需要登录态。</p>
 */
@Tag(name = "通知公告", description = "公告 CRUD、登录弹窗、Dashboard 摘要、已读标记")
@RestController
@RequestMapping("/system/notice")
@Validated
public class NoticeController {

    private final NoticeService noticeService;

    public NoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    /**
     * 公告分页（管理端）。
     */
    @Operation(summary = "公告分页", description = "按标题模糊 + 分类 + 状态过滤")
    @HasPermission("system:notice:list")
    @GetMapping
    public R<PageResp<NoticeResp>> page(NoticeQuery query,
                                        @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") int page,
                                        @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size) {
        return R.ok(noticeService.page(query, page, size));
    }

    /**
     * 公告详情。
     */
    @Operation(summary = "公告详情")
    @HasPermission("system:notice:list")
    @GetMapping("/{id}")
    public R<NoticeDetailResp> get(@Parameter(description = "公告 ID") @PathVariable Long id) {
        return R.ok(noticeService.get(id));
    }

    /**
     * 新增公告。
     */
    @Operation(summary = "新增公告")
    @HasPermission("system:notice:add")
    @PostMapping
    public R<Long> create(@RequestBody @Valid NoticeReq req) {
        return R.ok(noticeService.create(req));
    }

    /**
     * 修改公告（已发布的公告不允许修改通知范围 / 方式 / 定时）。
     */
    @Operation(summary = "修改公告")
    @HasPermission("system:notice:update")
    @PutMapping("/{id}")
    public R<Void> update(@Parameter(description = "公告 ID") @PathVariable Long id,
                          @RequestBody @Valid NoticeReq req) {
        noticeService.update(id, req);
        return R.ok();
    }

    /**
     * 批量删除公告。
     */
    @Operation(summary = "批量删除公告", description = "请求体形如 {\"ids\":[1,2,3]}")
    @HasPermission("system:notice:delete")
    @DeleteMapping
    public R<Void> delete(@RequestBody @Valid DeleteIdsReq req) {
        noticeService.delete(req.getIds());
        return R.ok();
    }

    /**
     * 登录弹窗公告（当前用户）：返回还未读且 method 含 POPUP 的已发布公告。
     */
    @Operation(summary = "登录弹窗公告", description = "登录用户专用：返回未读的 POPUP 公告，按发布时间倒序")
    @GetMapping("/popup")
    public R<List<NoticeDetailResp>> popup() {
        return R.ok(noticeService.listPopupForCurrentUser());
    }

    /**
     * 标记当前用户已读某条公告。
     */
    @Operation(summary = "标记公告已读")
    @PostMapping("/{id}/read")
    public R<Void> read(@Parameter(description = "公告 ID") @PathVariable Long id) {
        noticeService.read(id);
        return R.ok();
    }

    /**
     * Dashboard 最新公告摘要。
     */
    @Operation(summary = "Dashboard 最新公告", description = "默认 5 条，最大 50；按置顶 + 发布时间倒序")
    @GetMapping("/dashboard")
    public R<List<DashboardNoticeResp>> dashboard(@Parameter(description = "摘要条数")
                                                  @RequestParam(defaultValue = "5") int limit) {
        return R.ok(noticeService.listDashboard(limit));
    }

    /**
     * 通用的 “按 ID 列表批量删除” 请求体。
     */
    @Schema(description = "按 ID 列表批量删除请求体")
    public static class DeleteIdsReq {

        @Schema(description = "待删除的 ID 列表", example = "[1,2,3]", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty(message = "ID 列表不能为空")
        private List<Long> ids;

        public List<Long> getIds() {
            return ids;
        }

        public void setIds(List<Long> ids) {
            this.ids = ids;
        }
    }
}
