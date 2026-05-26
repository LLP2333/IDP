package com.qvqw.idp.storage.internal;

import com.qvqw.idp.common.exception.BusinessException;
import com.qvqw.idp.storage.PartETag;
import com.qvqw.idp.storage.Storage;
import com.qvqw.idp.storage.StorageHandler;
import com.qvqw.idp.storage.StoredObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * 本地文件系统存储句柄。
 *
 * <p>对象的实际物理路径 = {@code basePath + objectKey}；分片上传走 {@code basePath/.tmp/<uploadId>/<partNumber>.part}
 * 临时分片目录，complete 时按 partNumber 顺序合并到目标文件再清理临时目录。</p>
 */
class LocalStorageHandler implements StorageHandler {

    private static final String TMP_DIR = ".tmp";

    private final Path basePath;
    private final String urlPrefix;

    public LocalStorageHandler(Storage storage) {
        if (storage.getBucketName() == null || storage.getBucketName().isBlank()) {
            throw new BusinessException("本地存储路径未配置");
        }
        this.basePath = Paths.get(storage.getBucketName()).toAbsolutePath();
        this.urlPrefix = storage.resolveUrlPrefix();
        try {
            Files.createDirectories(basePath);
        } catch (IOException e) {
            throw new BusinessException("初始化本地存储目录失败: " + basePath);
        }
    }

    @Override
    public StoredObject upload(InputStream in, String objectKey, long size, String contentType) {
        Path target = resolve(objectKey);
        try {
            ensureParent(target);
            try (OutputStream out = Files.newOutputStream(target, StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
                MessageDigest md = MessageDigest.getInstance("SHA-1");
                byte[] buf = new byte[8192];
                long totalSize = 0;
                int n;
                while ((n = in.read(buf)) > 0) {
                    out.write(buf, 0, n);
                    md.update(buf, 0, n);
                    totalSize += n;
                }
                return new StoredObject(objectKey, toHex(md.digest()), totalSize, contentType);
            }
        } catch (Exception e) {
            throw new BusinessException("写入本地文件失败: " + e.getMessage());
        }
    }

    @Override
    public void delete(String objectKey) {
        Path target = resolve(objectKey);
        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            throw new BusinessException("删除本地文件失败: " + e.getMessage());
        }
    }

    @Override
    public void move(String fromKey, String toKey) {
        Path from = resolve(fromKey);
        Path to = resolve(toKey);
        try {
            if (!Files.exists(from)) {
                return;
            }
            ensureParent(to);
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BusinessException("移动本地文件失败: " + e.getMessage());
        }
    }

    @Override
    public InputStream download(String objectKey) {
        Path target = resolve(objectKey);
        try {
            return Files.newInputStream(target, StandardOpenOption.READ);
        } catch (IOException e) {
            throw new BusinessException("读取本地文件失败: " + e.getMessage());
        }
    }

    @Override
    public boolean exists(String objectKey) {
        return Files.exists(resolve(objectKey));
    }

    @Override
    public String resolveUrl(String objectKey) {
        if (objectKey == null || objectKey.isEmpty()) {
            return urlPrefix;
        }
        String key = objectKey.startsWith("/") ? objectKey.substring(1) : objectKey;
        return urlPrefix + key;
    }

    @Override
    public String initMultipartUpload(String objectKey, String contentType) {
        String uploadId = UUID.randomUUID().toString().replace("-", "");
        Path dir = tmpDir(uploadId);
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new BusinessException("初始化分片上传失败: " + e.getMessage());
        }
        return uploadId;
    }

    @Override
    public String uploadPart(String uploadId, String objectKey, int partNumber, InputStream in, long size) {
        Path partFile = tmpDir(uploadId).resolve(partNumber + ".part");
        try {
            ensureParent(partFile);
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            try (OutputStream out = Files.newOutputStream(partFile, StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) {
                    out.write(buf, 0, n);
                    md.update(buf, 0, n);
                }
            }
            return toHex(md.digest());
        } catch (Exception e) {
            throw new BusinessException("写入分片失败: " + e.getMessage());
        }
    }

    @Override
    public StoredObject completeMultipartUpload(String uploadId, String objectKey, List<PartETag> parts) {
        Path target = resolve(objectKey);
        Path dir = tmpDir(uploadId);
        try {
            ensureParent(target);
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            long total = 0;
            try (OutputStream out = Files.newOutputStream(target, StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
                List<PartETag> sorted = parts.stream()
                        .sorted(Comparator.comparingInt(PartETag::partNumber))
                        .toList();
                byte[] buf = new byte[8192];
                for (PartETag part : sorted) {
                    Path partFile = dir.resolve(part.partNumber() + ".part");
                    if (!Files.exists(partFile)) {
                        throw new BusinessException("分片缺失: partNumber=" + part.partNumber());
                    }
                    try (InputStream in = Files.newInputStream(partFile)) {
                        int n;
                        while ((n = in.read(buf)) > 0) {
                            out.write(buf, 0, n);
                            md.update(buf, 0, n);
                            total += n;
                        }
                    }
                }
            }
            cleanupTmp(dir);
            return new StoredObject(objectKey, toHex(md.digest()), total, null);
        } catch (Exception e) {
            throw new BusinessException("合并分片失败: " + e.getMessage());
        }
    }

    @Override
    public void abortMultipartUpload(String uploadId, String objectKey) {
        cleanupTmp(tmpDir(uploadId));
    }

    @Override
    public void close() {
        // 本地存储无需关闭
    }

    /**
     * 把 objectKey 解析为绝对路径，并防止越权访问（{@code ..} 跳出 basePath）。
     */
    private Path resolve(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            throw new BusinessException("对象 key 不能为空");
        }
        String normalized = objectKey.startsWith("/") ? objectKey.substring(1) : objectKey;
        Path target = basePath.resolve(normalized).normalize();
        if (!target.startsWith(basePath)) {
            throw new BusinessException("非法对象 key: " + objectKey);
        }
        return target;
    }

    private Path tmpDir(String uploadId) {
        return basePath.resolve(TMP_DIR).resolve(uploadId).normalize();
    }

    private static void ensureParent(Path p) throws IOException {
        Path parent = p.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    private static void cleanupTmp(Path dir) {
        if (!Files.exists(dir)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                    // best effort
                }
            });
        } catch (IOException ignored) {
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
