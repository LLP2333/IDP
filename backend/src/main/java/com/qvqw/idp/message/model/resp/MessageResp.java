package com.qvqw.idp.message.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 站内消息响应 DTO。
 *
 * <p>{@code isRead} / {@code readTime} 来自当前请求用户对应的 {@code MessageLog} 行。</p>
 */
@Schema(description = "站内消息")
public class MessageResp {

    @Schema(description = "消息 ID", example = "1")
    private Long id;

    @Schema(description = "消息类型（1=系统消息）", example = "1")
    private Integer type;

    @Schema(description = "标题", example = "公告通知")
    private String title;

    @Schema(description = "正文")
    private String content;

    @Schema(description = "业务跳转路径", example = "/admin/system/notice/view?id=1")
    private String path;

    @Schema(description = "是否已读")
    private Boolean isRead;

    @Schema(description = "已读时间（未读时为 null）")
    private LocalDateTime readTime;

    @Schema(description = "消息创建时间")
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

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

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Boolean getIsRead() {
        return isRead;
    }

    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }

    public LocalDateTime getReadTime() {
        return readTime;
    }

    public void setReadTime(LocalDateTime readTime) {
        this.readTime = readTime;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
