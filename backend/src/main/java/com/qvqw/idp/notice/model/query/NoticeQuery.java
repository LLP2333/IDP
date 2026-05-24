package com.qvqw.idp.notice.model.query;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 公告查询条件。
 */
@Schema(description = "公告查询条件")
public class NoticeQuery {

    @Schema(description = "标题（模糊匹配）")
    private String title;

    @Schema(description = "分类（取值于字典 notice_type）", example = "1")
    private String type;

    @Schema(description = "状态过滤（1=草稿, 2=待发布, 3=已发布）", example = "3")
    private Integer status;

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

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
