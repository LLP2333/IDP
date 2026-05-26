package com.qvqw.idp.storage.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 存储响应 DTO。
 *
 * <p>{@code secretKey} 字段对客户端永远脱敏为 {@code "******"}，避免在 GET 响应中泄露密文。</p>
 */
@Schema(description = "存储响应")
public class StorageResp {

    @Schema(description = "ID", example = "1")
    private Long id;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "编码")
    private String code;

    @Schema(description = "类型：1=本地，2=S3", example = "1")
    private Integer type;

    @Schema(description = "S3 Access Key（脱敏：仅 type=2 时返回原值）")
    private String accessKey;

    @Schema(description = "S3 Secret Key（永远返回 '******'）", example = "******")
    private String secretKey;

    @Schema(description = "S3 endpoint")
    private String endpoint;

    @Schema(description = "桶名 / 本地存储路径")
    private String bucketName;

    @Schema(description = "公开访问域名 / URL 前缀")
    private String domain;

    @Schema(description = "是否启用回收站")
    private Boolean recycleBinEnabled;

    @Schema(description = "回收站路径")
    private String recycleBinPath;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "是否为默认存储")
    private Boolean isDefault;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "状态：1=启用，2=禁用")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public Boolean getRecycleBinEnabled() {
        return recycleBinEnabled;
    }

    public void setRecycleBinEnabled(Boolean recycleBinEnabled) {
        this.recycleBinEnabled = recycleBinEnabled;
    }

    public String getRecycleBinPath() {
        return recycleBinPath;
    }

    public void setRecycleBinPath(String recycleBinPath) {
        this.recycleBinPath = recycleBinPath;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
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
