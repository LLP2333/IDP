package com.qvqw.idp.notice.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 公告详情响应 DTO，比列表多 {@code content} 与 {@code noticeUsers}。
 */
@Schema(description = "公告详情")
public class NoticeDetailResp extends NoticeResp {

    @Schema(description = "正文（纯文本 / Markdown）")
    private String content;

    @Schema(description = "指定通知用户 ID 列表（noticeScope=ALL 时为 null）")
    private List<Long> noticeUsers;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<Long> getNoticeUsers() {
        return noticeUsers;
    }

    public void setNoticeUsers(List<Long> noticeUsers) {
        this.noticeUsers = noticeUsers;
    }
}
