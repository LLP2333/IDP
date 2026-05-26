package com.qvqw.idp.file.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 分片上传初始化请求体。
 *
 * <p>当 {@code sha256} 命中已有文件时直接走秒传，跳过 init 步骤；否则后端会在 Redis 中
 * 暂存 {@code uploadId → 状态} 并返回。</p>
 */
@Schema(description = "分片上传初始化请求")
public class MultipartUploadInitReq {

    @Schema(description = "原始文件名（含扩展名）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "文件名不能为空")
    private String fileName;

    @Schema(description = "文件总大小（字节）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "文件大小不能为空")
    @Min(value = 1, message = "文件大小必须大于 0")
    private Long fileSize;

    @Schema(description = "分片大小（字节）", example = "5242880", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "分片大小不能为空")
    @Min(value = 1024 * 1024, message = "分片大小至少 1MB")
    private Long chunkSize;

    @Schema(description = "文件 SHA256 指纹（用于秒传）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "SHA256 不能为空")
    private String sha256;

    @Schema(description = "上传到的上级目录路径", example = "/")
    private String parentPath;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public Long getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(Long chunkSize) {
        this.chunkSize = chunkSize;
    }

    public String getSha256() {
        return sha256;
    }

    public void setSha256(String sha256) {
        this.sha256 = sha256;
    }

    public String getParentPath() {
        return parentPath;
    }

    public void setParentPath(String parentPath) {
        this.parentPath = parentPath;
    }
}
