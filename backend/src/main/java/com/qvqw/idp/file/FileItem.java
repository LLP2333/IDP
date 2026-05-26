package com.qvqw.idp.file;

import com.qvqw.idp.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * 文件 / 文件夹实体（{@code idp_sys_file}）。
 *
 * <p>同一张表记录文件与文件夹，{@code type=DIR} 时表示文件夹。软删除字段
 * {@code deleted}：0 表示正常，1 表示已放入回收站；JPA 不开启逻辑删除拦截，由业务层
 * 显式过滤。</p>
 *
 * <p>类名取 {@code FileItem} 而非 {@code File}，避免与 {@link java.io.File} 混淆。</p>
 */
@Entity
@Table(name = "idp_sys_file", indexes = {
        @Index(name = "idx_idp_sys_file_parent", columnList = "parent_path"),
        @Index(name = "idx_idp_sys_file_storage", columnList = "storage_id"),
        @Index(name = "idx_idp_sys_file_type", columnList = "type"),
        @Index(name = "idx_idp_sys_file_sha256", columnList = "sha256"),
        @Index(name = "idx_idp_sys_file_deleted", columnList = "deleted")
})
public class FileItem extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 存储侧文件名（自动生成，带扩展名）。 */
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    /** 原始文件名（用户上传时的名字）。 */
    @Column(name = "original_name", nullable = false, length = 500)
    private String originalName;

    /** 大小（字节，文件夹默认 0）。 */
    @Column(name = "size", nullable = false)
    private Long size = 0L;

    /** 上级目录绝对路径，以 {@code /} 开头不以 {@code /} 结尾，根目录为 {@code /}。 */
    @Column(name = "parent_path", nullable = false, length = 1000)
    private String parentPath;

    /** 完整路径 = {@code parentPath + '/' + name}。 */
    @Column(name = "path", nullable = false, length = 1500)
    private String path;

    /** 扩展名（不带 {@code .}，文件夹为空字符串）。 */
    @Column(name = "extension", length = 30)
    private String extension;

    /** MIME 类型。 */
    @Column(name = "content_type", length = 100)
    private String contentType;

    /** 文件类型整型值（{@link FileTypeEnum#getValue()}）。 */
    @Column(name = "type", nullable = false)
    private Integer type;

    /** SHA256 文件指纹，用于秒传。 */
    @Column(name = "sha256", length = 64)
    private String sha256;

    /** 元数据 JSON 字符串（用于存储宽高、码率等）。 */
    @Column(name = "metadata", columnDefinition = "text")
    private String metadata;

    /** 缩略图文件名（仅图片有，落在与文件相同目录）。 */
    @Column(name = "thumbnail_name", length = 255)
    private String thumbnailName;

    /** 缩略图大小（字节）。 */
    @Column(name = "thumbnail_size")
    private Long thumbnailSize;

    /** 关联的存储 ID。 */
    @Column(name = "storage_id", nullable = false)
    private Long storageId;

    /** 软删除标记：0=正常，1=回收站。 */
    @Column(name = "deleted", nullable = false)
    private Integer deleted = 0;

    /** 放入回收站的用户 ID。 */
    @Column(name = "deleted_by")
    private Long deletedBy;

    /** 放入回收站的时间。 */
    @Column(name = "deleted_at")
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

    public String getParentPath() {
        return parentPath;
    }

    /**
     * 设置上级目录，同时刷新 {@link #path}。
     */
    public void setParentPath(String parentPath) {
        this.parentPath = parentPath;
        if (this.name != null) {
            this.path = "/".equals(parentPath) ? "/" + this.name : parentPath + "/" + this.name;
        }
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

    public String getThumbnailName() {
        return thumbnailName;
    }

    public void setThumbnailName(String thumbnailName) {
        this.thumbnailName = thumbnailName;
    }

    public Long getThumbnailSize() {
        return thumbnailSize;
    }

    public void setThumbnailSize(Long thumbnailSize) {
        this.thumbnailSize = thumbnailSize;
    }

    public Long getStorageId() {
        return storageId;
    }

    public void setStorageId(Long storageId) {
        this.storageId = storageId;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    public Long getDeletedBy() {
        return deletedBy;
    }

    public void setDeletedBy(Long deletedBy) {
        this.deletedBy = deletedBy;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
}
