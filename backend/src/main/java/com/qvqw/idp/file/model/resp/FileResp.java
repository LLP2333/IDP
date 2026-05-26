package com.qvqw.idp.file.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 文件 / 文件夹响应 DTO。
 */
@Schema(description = "文件 / 文件夹响应")
public class FileResp {

    @Schema(description = "ID", example = "1")
    private Long id;

    @Schema(description = "存储侧文件名")
    private String name;

    @Schema(description = "原始名称")
    private String originalName;

    @Schema(description = "大小（字节）")
    private Long size;

    @Schema(description = "公开访问 URL（文件夹为空）")
    private String url;

    @Schema(description = "缩略图 URL（仅图片）")
    private String thumbnailUrl;

    @Schema(description = "上级目录绝对路径")
    private String parentPath;

    @Schema(description = "完整路径")
    private String path;

    @Schema(description = "扩展名")
    private String extension;

    @Schema(description = "MIME")
    private String contentType;

    @Schema(description = "文件类型：0=目录,1=其他,2=图片,3=文档,4=视频,5=音频")
    private Integer type;

    @Schema(description = "SHA256")
    private String sha256;

    @Schema(description = "元数据 JSON 字符串")
    private String metadata;

    @Schema(description = "存储 ID")
    private Long storageId;

    @Schema(description = "存储名称（用于列表展示）")
    private String storageName;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    @Schema(description = "进入回收站的时间（仅回收站查询返回）")
    private LocalDateTime deletedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
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

    public String getParentPath() {
        return parentPath;
    }

    public void setParentPath(String parentPath) {
        this.parentPath = parentPath;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getSha256() {
        return sha256;
    }

    public void setSha256(String sha256) {
        this.sha256 = sha256;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public Long getStorageId() {
        return storageId;
    }

    public void setStorageId(Long storageId) {
        this.storageId = storageId;
    }

    public String getStorageName() {
        return storageName;
    }

    public void setStorageName(String storageName) {
        this.storageName = storageName;
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

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
}
