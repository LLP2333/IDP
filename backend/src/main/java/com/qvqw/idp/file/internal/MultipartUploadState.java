package com.qvqw.idp.file.internal;

import java.util.ArrayList;
import java.util.List;

/**
 * 分片上传状态（序列化后存入 Redis 暂存）。
 *
 * <p>{@code parts} 保存按顺序累积的分片 ETag，complete 时直接交给底层存储 handler 合并。</p>
 */
public class MultipartUploadState {

    private String uploadId;
    private String objectKey;
    private String originalName;
    private String parentPath;
    private String contentType;
    private long fileSize;
    private String sha256;
    private Long storageId;
    private String extension;
    private List<Part> parts = new ArrayList<>();

    public String getUploadId() {
        return uploadId;
    }

    public void setUploadId(String uploadId) {
        this.uploadId = uploadId;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public String getParentPath() {
        return parentPath;
    }

    public void setParentPath(String parentPath) {
        this.parentPath = parentPath;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getSha256() {
        return sha256;
    }

    public void setSha256(String sha256) {
        this.sha256 = sha256;
    }

    public Long getStorageId() {
        return storageId;
    }

    public void setStorageId(Long storageId) {
        this.storageId = storageId;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public List<Part> getParts() {
        return parts;
    }

    public void setParts(List<Part> parts) {
        this.parts = parts;
    }

    /**
     * 单分片信息。
     */
    public static class Part {
        private int partNumber;
        private String etag;

        public Part() {
        }

        public Part(int partNumber, String etag) {
            this.partNumber = partNumber;
            this.etag = etag;
        }

        public int getPartNumber() {
            return partNumber;
        }

        public void setPartNumber(int partNumber) {
            this.partNumber = partNumber;
        }

        public String getEtag() {
            return etag;
        }

        public void setEtag(String etag) {
            this.etag = etag;
        }
    }
}
