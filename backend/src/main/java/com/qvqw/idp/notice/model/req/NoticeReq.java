package com.qvqw.idp.notice.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 公告新增 / 修改请求。
 *
 * <p>状态机：</p>
 * <ul>
 *   <li>{@code status=1 (DRAFT)} → 草稿，{@code publishTime} 忽略；</li>
 *   <li>{@code status=3 (PUBLISHED)} 且 {@code isTiming=false} → 立即发布，后端将
 *       {@code publishTime} 设为当前时间并触发 {@code MessageService.publish}；</li>
 *   <li>{@code status=3 (PUBLISHED)} 且 {@code isTiming=true} → 后端会改为
 *       {@code status=2 (PENDING)} 等待 {@code NoticeScheduler} 到点发布。</li>
 * </ul>
 */
@Schema(description = "公告新增/修改请求")
public class NoticeReq {

    @Schema(description = "标题", requiredMode = Schema.RequiredMode.REQUIRED, example = "这是公告标题")
    @NotBlank(message = "标题不能为空")
    @Size(max = 150, message = "标题长度不能超过 {max}")
    private String title;

    @Schema(description = "正文（纯文本 / Markdown）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "正文不能为空")
    private String content;

    @Schema(description = "分类（取值于字典 notice_type）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotBlank(message = "分类不能为空")
    @Size(max = 30, message = "分类长度不能超过 {max}")
    private String type;

    @Schema(description = "通知范围（1=所有人, 2=指定用户）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "通知范围不能为空")
    private Integer noticeScope;

    @Schema(description = "通知用户 ID 列表（noticeScope=2 时必填）")
    private List<Long> noticeUsers;

    @Schema(description = "通知方式列表（1=系统消息, 2=登录弹窗）", example = "[1]")
    private List<Integer> noticeMethods;

    @Schema(description = "是否定时发布", requiredMode = Schema.RequiredMode.REQUIRED, example = "false")
    @NotNull(message = "是否定时不能为空")
    private Boolean isTiming;

    @Schema(description = "发布时间（isTiming=true 时必填且必须晚于现在）", example = "2026-06-01 10:00:00")
    private LocalDateTime publishTime;

    @Schema(description = "是否置顶", example = "false")
    private Boolean isTop;

    @Schema(description = "目标状态（1=草稿, 3=发布；后端会按 isTiming 自动改为 2=待发布）", example = "3")
    private Integer status;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getNoticeScope() {
        return noticeScope;
    }

    public void setNoticeScope(Integer noticeScope) {
        this.noticeScope = noticeScope;
    }

    public List<Long> getNoticeUsers() {
        return noticeUsers;
    }

    public void setNoticeUsers(List<Long> noticeUsers) {
        this.noticeUsers = noticeUsers;
    }

    public List<Integer> getNoticeMethods() {
        return noticeMethods;
    }

    public void setNoticeMethods(List<Integer> noticeMethods) {
        this.noticeMethods = noticeMethods;
    }

    public Boolean getIsTiming() {
        return isTiming;
    }

    public void setIsTiming(Boolean isTiming) {
        this.isTiming = isTiming;
    }

    public LocalDateTime getPublishTime() {
        return publishTime;
    }

    public void setPublishTime(LocalDateTime publishTime) {
        this.publishTime = publishTime;
    }

    public Boolean getIsTop() {
        return isTop;
    }

    public void setIsTop(Boolean isTop) {
        this.isTop = isTop;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
