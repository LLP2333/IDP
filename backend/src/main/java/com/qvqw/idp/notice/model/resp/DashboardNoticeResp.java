package com.qvqw.idp.notice.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Dashboard 摘要响应：最新公告卡片所需的最小字段集合。
 */
@Schema(description = "Dashboard 公告摘要")
public class DashboardNoticeResp {

    @Schema(description = "公告 ID", example = "1")
    private Long id;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "分类（取值于字典 notice_type）", example = "1")
    private String type;

    @Schema(description = "是否置顶")
    private Boolean isTop;

    @Schema(description = "发布时间")
    private LocalDateTime publishTime;

    @Schema(description = "对当前用户是否已读")
    private Boolean isRead;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Boolean getIsTop() {
        return isTop;
    }

    public void setIsTop(Boolean isTop) {
        this.isTop = isTop;
    }

    public LocalDateTime getPublishTime() {
        return publishTime;
    }

    public void setPublishTime(LocalDateTime publishTime) {
        this.publishTime = publishTime;
    }

    public Boolean getIsRead() {
        return isRead;
    }

    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }
}
