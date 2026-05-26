package com.qvqw.idp.file;

import com.qvqw.idp.file.model.req.MultipartUploadInitReq;
import com.qvqw.idp.file.model.resp.FileResp;
import com.qvqw.idp.file.model.resp.MultipartUploadInitResp;
import com.qvqw.idp.file.model.resp.MultipartUploadPartResp;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 分片上传对外服务接口。
 *
 * <p>客户端调用顺序：</p>
 * <ol>
 *   <li>{@link #init(MultipartUploadInitReq)} → 命中 SHA256 走秒传；否则拿到 uploadId；</li>
 *   <li>循环 {@link #uploadPart(String, int, MultipartFile)} 上传每一分片；</li>
 *   <li>{@link #complete(String)} 合并并落库；</li>
 *   <li>失败或取消时 {@link #cancel(String)} 释放底层资源。</li>
 * </ol>
 */
public interface MultipartUploadService {

    /**
     * 初始化分片上传。
     *
     * @param req 请求体
     * @return 初始化响应（含 uploadId 或秒传命中的已有文件）
     */
    MultipartUploadInitResp init(MultipartUploadInitReq req);

    /**
     * 上传一个分片。
     *
     * @param uploadId   uploadId
     * @param partNumber 分片编号（从 1 开始）
     * @param part       分片内容
     * @return 分片 ETag
     * @throws IOException 读取流失败时
     */
    MultipartUploadPartResp uploadPart(String uploadId, int partNumber, MultipartFile part) throws IOException;

    /**
     * 完成分片上传：合并 + 落库。
     *
     * @param uploadId uploadId
     * @return 最终文件
     */
    FileResp complete(String uploadId);

    /**
     * 取消分片上传：通知底层存储 abort + 删除 Redis 状态。
     *
     * @param uploadId uploadId
     */
    void cancel(String uploadId);
}
