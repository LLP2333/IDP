package com.qvqw.idp.notice.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 公告列表响应 DTO。
 *
 * <p>列表场景不返回正文 {@code content}，详情接口才返回。</p>
 */
@Schema(description = "公告列表项")
public class NoticeResp {

    @Schema(description = "公告 ID", example = "1")
    private Long id;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "分类（取值于字典 notice_type）", example = "1")
    private String type;

    @Schema(description = "通知范围（1=所有人, 2=指定用户）", example = "1")
    private Integer noticeScope;

    @Schema(description = "通知方式列表（1=系统消息, 2=登录弹窗）", example = "[1,2]")
    private List<Integer> noticeMethods;

    @Schema(description = "是否定时发布")
    private Boolean isTiming;

    @Schema(description = "发布时间（草稿可能为空）")
    private LocalDateTime publishTime;

    @Schema(description = "是否置顶")
    private Boolean isTop;

    @Schema(description = "状态（1=草稿, 2=待发布, 3=已发布）", example = "3")
    private Integer status;

    @Schema(description = "对当前用户是否已读；管理端列表不计算时为 null", example = "false")
    private Boolean isRead;

    @Schema(description = "发布人显示名（nickname 优先）")
    private String createUserString;

    @Schema(description = "创建人 ID")
    private Long createdBy;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "最后更新时间")
    private LocalDateTime updatedAt;

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

    public Integer getNoticeScope() {
        return noticeScope;
    }

    public void setNoticeScope(Integer noticeScope) {
        this.noticeScope = noticeScope;
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

    public Boolean getIsRead() {
        return isRead;
    }

    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }

    public String getCreateUserString() {
        return createUserString;
    }

    public void setCreateUserString(String createUserString) {
        this.createUserString = createUserString;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
