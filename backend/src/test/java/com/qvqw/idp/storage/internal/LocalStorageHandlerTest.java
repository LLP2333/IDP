package com.qvqw.idp.storage.internal;

import com.qvqw.idp.storage.PartETag;
import com.qvqw.idp.storage.Storage;
import com.qvqw.idp.storage.StorageHandler;
import com.qvqw.idp.storage.StoredObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link LocalStorageHandler} 单元测试，验证文件读写、移动、分片合并。
 */
class LocalStorageHandlerTest {

    @TempDir
    Path tempDir;

    private LocalStorageHandler newHandler() {
        Storage storage = new Storage();
        storage.setId(1L);
        storage.setType(0);
        storage.setBucketName(tempDir.toString() + "/");
        storage.setDomain("http://localhost:8080/file/local/");
        return new LocalStorageHandler(storage);
    }

    @Test
    void resolveUrlShouldPrependDomain() {
        StorageHandler handler = newHandler();
        assertThat(handler.resolveUrl("2026/05/26/abc.png"))
                .isEqualTo("http://localhost:8080/file/local/2026/05/26/abc.png");
        assertThat(handler.resolveUrl("/2026/05/26/abc.png"))
                .isEqualTo("http://localhost:8080/file/local/2026/05/26/abc.png");
    }

    @Test
    void uploadAndDownloadRoundTrip() throws IOException {
        StorageHandler handler = newHandler();
        byte[] data = "hello local storage".getBytes(StandardCharsets.UTF_8);

        StoredObject obj = handler.upload(new ByteArrayInputStream(data), "a/b/test.txt", data.length, "text/plain");
        assertThat(obj.size()).isEqualTo(data.length);
        assertThat(obj.etag()).isNotNull();
        assertThat(handler.exists("a/b/test.txt")).isTrue();

        try (InputStream in = handler.download("a/b/test.txt")) {
            assertThat(new String(in.readAllBytes(), StandardCharsets.UTF_8))
                    .isEqualTo("hello local storage");
        }
    }

    @Test
    void deleteShouldRemoveFile() {
        StorageHandler handler = newHandler();
        byte[] data = "x".getBytes(StandardCharsets.UTF_8);
        handler.upload(new ByteArrayInputStream(data), "to-del.txt", data.length, "text/plain");
        assertThat(handler.exists("to-del.txt")).isTrue();

        handler.delete("to-del.txt");
        assertThat(handler.exists("to-del.txt")).isFalse();
    }

    @Test
    void moveShouldRelocateFile() throws IOException {
        StorageHandler handler = newHandler();
        byte[] data = "move-me".getBytes(StandardCharsets.UTF_8);
        handler.upload(new ByteArrayInputStream(data), "a.txt", data.length, "text/plain");

        handler.move("a.txt", "trash/a.txt");
        assertThat(handler.exists("a.txt")).isFalse();
        assertThat(handler.exists("trash/a.txt")).isTrue();
        try (InputStream in = handler.download("trash/a.txt")) {
            assertThat(new String(in.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("move-me");
        }
    }

    @Test
    void rejectPathTraversal() {
        StorageHandler handler = newHandler();
        assertThatThrownBy(() -> handler.upload(
                new ByteArrayInputStream(new byte[]{1}), "../../escape.txt", 1, null))
                .hasMessageContaining("非法对象 key");
    }

    @Test
    void multipartUploadAndComplete() throws IOException {
        StorageHandler handler = newHandler();
        String objectKey = "merged.bin";
        String uploadId = handler.initMultipartUpload(objectKey, "application/octet-stream");

        byte[] part1 = "hello-".getBytes(StandardCharsets.UTF_8);
        byte[] part2 = "world".getBytes(StandardCharsets.UTF_8);
        String etag1 = handler.uploadPart(uploadId, objectKey, 1, new ByteArrayInputStream(part1), part1.length);
        String etag2 = handler.uploadPart(uploadId, objectKey, 2, new ByteArrayInputStream(part2), part2.length);
        assertThat(etag1).isNotBlank();
        assertThat(etag2).isNotBlank();

        List<PartETag> parts = new ArrayList<>();
        parts.add(new PartETag(2, etag2));
        parts.add(new PartETag(1, etag1));
        StoredObject obj = handler.completeMultipartUpload(uploadId, objectKey, parts);

        assertThat(obj.size()).isEqualTo(part1.length + part2.length);
        try (InputStream in = handler.download(objectKey)) {
            assertThat(new String(in.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("hello-world");
        }
    }

    @Test
    void abortMultipartShouldRemoveTmp() {
        StorageHandler handler = newHandler();
        String objectKey = "abort.bin";
        String uploadId = handler.initMultipartUpload(objectKey, null);
        handler.uploadPart(uploadId, objectKey, 1, new ByteArrayInputStream(new byte[]{1, 2, 3}), 3);
        handler.abortMultipartUpload(uploadId, objectKey);
        Path tmp = tempDir.resolve(".tmp").resolve(uploadId);
        assertThat(Files.exists(tmp)).isFalse();
    }
}
