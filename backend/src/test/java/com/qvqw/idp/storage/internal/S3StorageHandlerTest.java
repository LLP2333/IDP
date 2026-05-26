package com.qvqw.idp.storage.internal;

import com.qvqw.idp.storage.PartETag;
import com.qvqw.idp.storage.Storage;
import com.qvqw.idp.storage.StorageHandler;
import com.qvqw.idp.storage.StorageType;
import com.qvqw.idp.storage.StoredObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * {@link S3StorageHandler} 集成测试，使用 Testcontainers 启动真实 MinIO 容器。
 *
 * <p>Docker 环境不可用时（CI 无 docker / 沙箱）自动跳过测试，避免阻塞主流程。</p>
 */
@Testcontainers(disabledWithoutDocker = true)
class S3StorageHandlerTest {

    @Container
    static final MinIOContainer MINIO = new MinIOContainer("minio/minio:RELEASE.2024-12-18T13-15-44Z")
            .withUserName("idptest")
            .withPassword("idp-test-password");

    @BeforeAll
    static void ensureDocker() {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(), "需要 Docker 才能运行 S3 集成测试");
        MINIO.start();
    }

    private static StorageHandler buildHandler(String bucket) {
        // 通过 MinIO Admin API 创建 bucket 不便利，改成借助 AWS SDK 自身：
        software.amazon.awssdk.services.s3.S3Client adminClient = software.amazon.awssdk.services.s3.S3Client.builder()
                .endpointOverride(java.net.URI.create(MINIO.getS3URL()))
                .credentialsProvider(software.amazon.awssdk.auth.credentials.StaticCredentialsProvider.create(
                        software.amazon.awssdk.auth.credentials.AwsBasicCredentials.create(MINIO.getUserName(), MINIO.getPassword())))
                .region(software.amazon.awssdk.regions.Region.US_EAST_1)
                .serviceConfiguration(software.amazon.awssdk.services.s3.S3Configuration.builder()
                        .pathStyleAccessEnabled(true).build())
                .build();
        try {
            adminClient.createBucket(software.amazon.awssdk.services.s3.model.CreateBucketRequest.builder()
                    .bucket(bucket).build());
        } catch (software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException ignored) {
        } finally {
            adminClient.close();
        }

        Storage storage = new Storage();
        storage.setId(1L);
        storage.setType(StorageType.S3.getValue());
        storage.setAccessKey(MINIO.getUserName());
        storage.setEndpoint(MINIO.getS3URL());
        storage.setBucketName(bucket);
        storage.setDomain(MINIO.getS3URL() + "/" + bucket + "/");
        return new S3StorageHandler(storage, MINIO.getPassword());
    }

    @Test
    void uploadAndDownloadRoundTrip() throws IOException {
        try (StorageHandler handler = buildHandler("idp-test-1")) {
            byte[] data = "hello-s3".getBytes(StandardCharsets.UTF_8);
            StoredObject obj = handler.upload(new ByteArrayInputStream(data), "dir/a.txt", data.length, "text/plain");
            assertThat(obj.size()).isEqualTo(data.length);
            assertThat(handler.exists("dir/a.txt")).isTrue();

            try (InputStream in = handler.download("dir/a.txt")) {
                assertThat(new String(in.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("hello-s3");
            }

            handler.move("dir/a.txt", "trash/a.txt");
            assertThat(handler.exists("dir/a.txt")).isFalse();
            assertThat(handler.exists("trash/a.txt")).isTrue();

            handler.delete("trash/a.txt");
            assertThat(handler.exists("trash/a.txt")).isFalse();
        }
    }

    @Test
    void multipartUploadShouldWork() throws IOException {
        try (StorageHandler handler = buildHandler("idp-test-mu")) {
            String key = "mu/big.bin";
            String uploadId = handler.initMultipartUpload(key, "application/octet-stream");

            // 至少 5MB / part 才符合 MinIO 多分片要求（最后一个分片例外）
            byte[] p1 = new byte[5 * 1024 * 1024];
            byte[] p2 = new byte[1024];
            for (int i = 0; i < p1.length; i++) p1[i] = (byte) (i & 0xff);
            for (int i = 0; i < p2.length; i++) p2[i] = (byte) ((i + 1) & 0xff);

            String etag1 = handler.uploadPart(uploadId, key, 1, new ByteArrayInputStream(p1), p1.length);
            String etag2 = handler.uploadPart(uploadId, key, 2, new ByteArrayInputStream(p2), p2.length);

            StoredObject merged = handler.completeMultipartUpload(uploadId, key,
                    List.of(new PartETag(1, etag1), new PartETag(2, etag2)));
            assertThat(merged.size()).isEqualTo(p1.length + p2.length);

            handler.delete(key);
        }
    }
}
