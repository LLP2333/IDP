package com.qvqw.idp.file.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 普通上传响应 DTO。
 */
@Schema(description = "上传响应")
public class FileUploadResp {

    @Schema(description = "文件 ID")
    private Long id;

    @Schema(description = "文件 URL")
    private String url;

    @Schema(description = "缩略图 URL（仅图片）")
    private String thumbnailUrl;

    @Schema(description = "元数据 JSON 字符串")
    private String metadata;

    public FileUploadResp() {
    }

    public FileUploadResp(Long id, String url, String thumbnailUrl, String metadata) {
        this.id = id;
        this.url = url;
        this.thumbnailUrl = thumbnailUrl;
        this.metadata = metadata;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }
}
