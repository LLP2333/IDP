package com.qvqw.idp.file.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 分片上传初始化响应 DTO。
 *
 * <p>当命中 SHA256 秒传时 {@code existing} 字段返回已有文件信息，前端跳过分片步骤；
 * 否则 {@code uploadId} 非空，按 {@code chunkSize} 切片上传。</p>
 */
@Schema(description = "分片上传初始化响应")
public class MultipartUploadInitResp {

    @Schema(description = "上传 ID（命中秒传时为空）")
    private String uploadId;

    @Schema(description = "命中秒传时返回已有文件，否则为空")
    private FileResp existing;

    public String getUploadId() {
        return uploadId;
    }

    public void setUploadId(String uploadId) {
        this.uploadId = uploadId;
    }

    public FileResp getExisting() {
        return existing;
    }

    public void setExisting(FileResp existing) {
        this.existing = existing;
    }
}
