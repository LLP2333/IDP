package com.qvqw.idp.storage.internal;

import com.qvqw.idp.common.exception.BusinessException;
import com.qvqw.idp.storage.PartETag;
import com.qvqw.idp.storage.Storage;
import com.qvqw.idp.storage.StorageHandler;
import com.qvqw.idp.storage.StoredObject;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;

import java.io.InputStream;
import java.net.URI;
import java.util.Comparator;
import java.util.List;

/**
 * S3 协议存储句柄，基于 AWS SDK v2，兼容 MinIO / AWS S3 / OSS / COS。
 *
 * <p>MinIO 通常需要 Path-style URL，因此默认开启 {@code pathStyleAccessEnabled=true}。</p>
 */
class S3StorageHandler implements StorageHandler {

    private final S3Client client;
    private final String bucket;
    private final String urlPrefix;

    public S3StorageHandler(Storage storage, String decryptedSecretKey) {
        if (storage.getEndpoint() == null || storage.getEndpoint().isBlank()) {
            throw new BusinessException("S3 endpoint 未配置");
        }
        if (storage.getBucketName() == null || storage.getBucketName().isBlank()) {
            throw new BusinessException("S3 bucket 未配置");
        }
        this.bucket = storage.getBucketName();
        this.urlPrefix = storage.resolveUrlPrefix();
        this.client = S3Client.builder()
                .endpointOverride(URI.create(storage.getEndpoint()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(storage.getAccessKey(), decryptedSecretKey)))
                .region(Region.US_EAST_1)
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
    }

    @Override
    public StoredObject upload(InputStream in, String objectKey, long size, String contentType) {
        try {
            PutObjectRequest req = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .contentType(contentType)
                    .build();
            PutObjectResponse resp = client.putObject(req, RequestBody.fromInputStream(in, size));
            return new StoredObject(objectKey, resp.eTag(), size, contentType);
        } catch (Exception e) {
            throw new BusinessException("S3 上传失败: " + e.getMessage());
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(objectKey).build());
        } catch (Exception e) {
            throw new BusinessException("S3 删除失败: " + e.getMessage());
        }
    }

    @Override
    public void move(String fromKey, String toKey) {
        try {
            client.copyObject(CopyObjectRequest.builder()
                    .sourceBucket(bucket).sourceKey(fromKey)
                    .destinationBucket(bucket).destinationKey(toKey)
                    .build());
            client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(fromKey).build());
        } catch (NoSuchKeyException nsk) {
            // 源对象不存在视为成功
        } catch (Exception e) {
            throw new BusinessException("S3 移动失败: " + e.getMessage());
        }
    }

    @Override
    public InputStream download(String objectKey) {
        try {
            return client.getObject(GetObjectRequest.builder().bucket(bucket).key(objectKey).build(),
                    ResponseTransformer.toInputStream());
        } catch (Exception e) {
            throw new BusinessException("S3 下载失败: " + e.getMessage());
        }
    }

    @Override
    public boolean exists(String objectKey) {
        try {
            client.headObject(HeadObjectRequest.builder().bucket(bucket).key(objectKey).build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String resolveUrl(String objectKey) {
        String key = objectKey.startsWith("/") ? objectKey.substring(1) : objectKey;
        return urlPrefix + key;
    }

    @Override
    public String initMultipartUpload(String objectKey, String contentType) {
        try {
            CreateMultipartUploadResponse resp = client.createMultipartUpload(CreateMultipartUploadRequest.builder()
                    .bucket(bucket).key(objectKey).contentType(contentType).build());
            return resp.uploadId();
        } catch (Exception e) {
            throw new BusinessException("初始化分片上传失败: " + e.getMessage());
        }
    }

    @Override
    public String uploadPart(String uploadId, String objectKey, int partNumber, InputStream in, long size) {
        try {
            UploadPartResponse resp = client.uploadPart(UploadPartRequest.builder()
                    .bucket(bucket).key(objectKey).uploadId(uploadId).partNumber(partNumber)
                    .contentLength(size).build(), RequestBody.fromInputStream(in, size));
            return resp.eTag();
        } catch (Exception e) {
            throw new BusinessException("上传分片失败: " + e.getMessage());
        }
    }

    @Override
    public StoredObject completeMultipartUpload(String uploadId, String objectKey, List<PartETag> parts) {
        try {
            List<CompletedPart> completed = parts.stream()
                    .sorted(Comparator.comparingInt(PartETag::partNumber))
                    .map(p -> CompletedPart.builder().partNumber(p.partNumber()).eTag(p.etag()).build())
                    .toList();
            client.completeMultipartUpload(CompleteMultipartUploadRequest.builder()
                    .bucket(bucket).key(objectKey).uploadId(uploadId)
                    .multipartUpload(CompletedMultipartUpload.builder().parts(completed).build())
                    .build());
            long size = 0;
            String etag = null;
            try {
                var head = client.headObject(HeadObjectRequest.builder().bucket(bucket).key(objectKey).build());
                size = head.contentLength() == null ? 0 : head.contentLength();
                etag = head.eTag();
            } catch (Exception ignored) {
            }
            return new StoredObject(objectKey, etag, size, null);
        } catch (Exception e) {
            throw new BusinessException("合并分片失败: " + e.getMessage());
        }
    }

    @Override
    public void abortMultipartUpload(String uploadId, String objectKey) {
        try {
            client.abortMultipartUpload(AbortMultipartUploadRequest.builder()
                    .bucket(bucket).key(objectKey).uploadId(uploadId).build());
        } catch (Exception ignored) {
        }
    }

    @Override
    public void close() {
        client.close();
    }
}
