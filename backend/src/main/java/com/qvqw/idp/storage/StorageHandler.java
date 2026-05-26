package com.qvqw.idp.storage;

import java.io.InputStream;
import java.util.List;

/**
 * 存储引擎句柄接口。
 *
 * <p>{@code file} 模块通过 {@link StorageHandlerFactory#get(Storage)} 拿到 handler，再调用本接口
 * 提供的上传、下载、移动、删除、分片上传等操作。所有方法的 {@code objectKey} 表示对象在存储引擎内的相对路径
 * （不带前导 {@code /}，如 {@code 2025/05/foo.jpg}）。</p>
 */
public interface StorageHandler extends AutoCloseable {

    /**
     * 上传文件流到指定对象。
     *
     * @param in          输入流（由调用方负责关闭，方法内不应 close 它）
     * @param objectKey   对象 key
     * @param size        文件大小（字节）
     * @param contentType MIME
     * @return 上传结果（含 etag、最终大小）
     */
    StoredObject upload(InputStream in, String objectKey, long size, String contentType);

    /**
     * 删除对象（不存在时静默忽略）。
     *
     * @param objectKey 对象 key
     */
    void delete(String objectKey);

    /**
     * 移动对象，等价于复制 + 删除原对象。
     *
     * @param fromKey 源
     * @param toKey   目标
     */
    void move(String fromKey, String toKey);

    /**
     * 下载对象流；调用方负责关闭返回的流。
     *
     * @param objectKey 对象 key
     * @return 输入流
     */
    InputStream download(String objectKey);

    /**
     * 判断对象是否存在。
     *
     * @param objectKey 对象 key
     * @return 存在返回 true
     */
    boolean exists(String objectKey);

    /**
     * 拼接最终公开访问 URL（拼接 storage.domain / endpoint 前缀）。
     *
     * @param objectKey 对象 key
     * @return URL
     */
    String resolveUrl(String objectKey);

    /**
     * 初始化分片上传。
     *
     * @param objectKey   目标对象 key
     * @param contentType MIME
     * @return uploadId（对 LOCAL 是临时 UUID，对 S3 是 S3 返回的 uploadId）
     */
    String initMultipartUpload(String objectKey, String contentType);

    /**
     * 上传一个分片。
     *
     * @param uploadId   {@link #initMultipartUpload(String, String)} 返回的 ID
     * @param objectKey  目标对象 key
     * @param partNumber 分片编号（从 1 开始）
     * @param in         分片内容输入流
     * @param size       分片字节数
     * @return 分片 ETag（用于 complete 时传回）
     */
    String uploadPart(String uploadId, String objectKey, int partNumber, InputStream in, long size);

    /**
     * 合并分片，完成上传。
     *
     * @param uploadId  uploadId
     * @param objectKey 目标对象 key
     * @param parts     分片 ETag 列表（按 partNumber 升序）
     * @return 合并后的对象元信息
     */
    StoredObject completeMultipartUpload(String uploadId, String objectKey, List<PartETag> parts);

    /**
     * 中止分片上传（清理已上传分片与临时元数据）。
     *
     * @param uploadId  uploadId
     * @param objectKey 目标对象 key
     */
    void abortMultipartUpload(String uploadId, String objectKey);

    /**
     * 关闭底层资源；对 LOCAL 通常为 no-op，对 S3 会关闭 {@code S3Client}。
     */
    @Override
    void close();
}
