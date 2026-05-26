package com.qvqw.idp.storage.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 新增存储请求体。
 *
 * <p>LOCAL 与 S3 类型校验规则不同，业务层在 {@code StorageServiceImpl} 中按 {@code type} 分支校验。</p>
 */
@Schema(description = "新增存储请求")
public class StorageCreateReq {

    @Schema(description = "名称", example = "本地存储", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "名称不能为空")
    @Size(max = 100, message = "名称长度不能超过 100 个字符")
    private String name;

    @Schema(description = "编码（全局唯一）", example = "local", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "编码不能为空")
    @Size(max = 30, message = "编码长度不能超过 30 个字符")
    private String code;

    @Schema(description = "存储类型：1=本地，2=S3", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "存储类型不能为空")
    private Integer type;

    @Schema(description = "S3 Access Key，仅 type=2 时必填")
    private String accessKey;

    @Schema(description = "S3 Secret Key，仅 type=2 时新增必填；修改时留空表示不修改")
    private String secretKey;

    @Schema(description = "S3 endpoint，仅 type=2 时必填", example = "http://localhost:9000")
    private String endpoint;

    @Schema(description = "桶名（LOCAL 时为本地绝对路径）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "桶名 / 存储路径不能为空")
    private String bucketName;

    @Schema(description = "公开访问域名 / URL 前缀；LOCAL 必填")
    private String domain;

    @Schema(description = "是否启用回收站", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "是否启用回收站不能为空")
    private Boolean recycleBinEnabled;

    @Schema(description = "回收站相对路径（启用时必填）", example = ".RECYCLE.BIN/")
    private String recycleBinPath;

    @Schema(description = "描述")
    @Size(max = 500, message = "描述长度不能超过 500 个字符")
    private String description;

    @Schema(description = "排序", example = "999", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "排序不能为空")
    private Integer sort;

    @Schema(description = "状态：1=启用，2=禁用", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "状态不能为空")
    private Integer status;

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
}
