package com.qvqw.idp.storage;

/**
 * 存储引擎返回的对象元信息。
 *
 * @param objectKey   对象 key
 * @param etag        底层存储返回的 ETag（LOCAL 用 SHA1 hex；S3 用 MD5 双引号字符串）
 * @param size        对象大小（字节）
 * @param contentType MIME（未知时为 null）
 */
public record StoredObject(String objectKey, String etag, long size, String contentType) {
}
