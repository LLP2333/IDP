package com.qvqw.idp.message.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 内部模块发消息请求。
 *
 * <p>仅供 notice 等模块在服务层调用，不通过 HTTP 暴露。</p>
 */
@Schema(description = "消息发布请求")
public class MessageCreateReq {

    @Schema(description = "消息类型（1=系统消息）", example = "1")
    private Integer type = 1;

    @Schema(description = "标题", requiredMode = Schema.RequiredMode.REQUIRED, example = "公告通知")
    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题长度不能超过 {max}")
    private String title;

    @Schema(description = "正文", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "正文不能为空")
    private String content;

    @Schema(description = "业务跳转路径")
    @Size(max = 255, message = "跳转路径长度不能超过 {max}")
    private String path;

    public MessageCreateReq() {
    }

    public MessageCreateReq(String title, String content, String path) {
        this.title = title;
        this.content = content;
        this.path = path;
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
}
