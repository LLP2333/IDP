package com.qvqw.idp.storage;

/**
 * 分片上传 ETag。
 *
 * @param partNumber 分片编号（从 1 开始）
 * @param etag       底层存储返回的 ETag
 */
public record PartETag(int partNumber, String etag) {
}
