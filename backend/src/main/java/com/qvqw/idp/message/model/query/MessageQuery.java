package com.qvqw.idp.message.model.query;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 消息收件箱查询条件。
 */
@Schema(description = "消息查询条件")
public class MessageQuery {

    @Schema(description = "标题（模糊匹配）", example = "公告")
    private String title;

    @Schema(description = "是否已读（不传则返回全部）", example = "false")
    private Boolean isRead;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Boolean getIsRead() {
        return isRead;
    }

    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }
}
