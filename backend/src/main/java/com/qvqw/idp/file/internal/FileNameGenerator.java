package com.qvqw.idp.file.internal;

import com.qvqw.idp.file.FileItem;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 文件名生成工具。
 *
 * <p>负责：</p>
 * <ul>
 *   <li>按日期生成存储侧 objectKey 前缀（{@code yyyy/MM/dd/}）；</li>
 *   <li>为重名文件生成 {@code name(1).ext} 之类的占位名；</li>
 *   <li>为存储侧的实际对象生成唯一名（用随机串避免冲突）。</li>
 * </ul>
 */
final class FileNameGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter DATE_PATH = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private FileNameGenerator() {
    }

    /**
     * 生成日期 objectKey 前缀，形如 {@code 2025/05/25/}。
     */
    static String datePrefix() {
        return LocalDate.now().format(DATE_PATH) + "/";
    }

    /**
     * 为存储侧生成随机文件名：32 字符十六进制 + 扩展名。
     *
     * @param extension 扩展名（不带 {@code .}）
     * @return 唯一文件名
     */
    static String generateStorageName(String extension) {
        byte[] buf = new byte[16];
        RANDOM.nextBytes(buf);
        StringBuilder sb = new StringBuilder(32);
        for (byte b : buf) {
            sb.append(HEX[(b >> 4) & 0xF]);
            sb.append(HEX[b & 0xF]);
        }
        if (extension != null && !extension.isEmpty()) {
            sb.append('.').append(extension);
        }
        return sb.toString();
    }

    /**
     * 在同一目录下检查并生成不冲突的展示名：file.txt 已存在则返回 file(1).txt。
     *
     * @param originalName 原始名
     * @param parentPath   父目录
     * @param storageId    存储 ID
     * @param repository   仓库
     * @return 不冲突的展示名
     */
    static String resolveUniqueOriginalName(String originalName, String parentPath, Long storageId,
                                            FileRepository repository) {
        String base = originalName;
        String ext = "";
        int dot = originalName.lastIndexOf('.');
        if (dot > 0) {
            base = originalName.substring(0, dot);
            ext = originalName.substring(dot);
        }
        String candidate = originalName;
        int n = 0;
        while (exists(parentPath, candidate, storageId, repository)) {
            n++;
            candidate = base + "(" + n + ")" + ext;
        }
        return candidate;
    }

    private static boolean exists(String parentPath, String name, Long storageId, FileRepository repository) {
        return repository.findAllByParentPathAndDeleted(parentPath, 0).stream()
                .anyMatch(f -> name.equals(f.getOriginalName())
                        && storageId.equals(f.getStorageId())
                        && f.getType() != 0);
    }

    /**
     * 从文件名提取扩展名（小写），无扩展名返回空字符串。
     */
    static String extOf(String fileName) {
        if (fileName == null) {
            return "";
        }
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dot + 1).toLowerCase();
    }

    /**
     * 根据已知文件实体 + 新名生成完整 path（父目录 + 名字）。
     */
    static String composePath(String parentPath, String name) {
        if ("/".equals(parentPath)) {
            return "/" + name;
        }
        return parentPath + "/" + name;
    }

    /**
     * 拼接 objectKey：{@code parentPath + storageName}。
     * 对 parentPath 做规范：去前导 {@code /}、保留尾部 {@code /}。
     */
    static String composeObjectKey(String parentPath, String storageName) {
        if (parentPath == null || parentPath.isEmpty() || "/".equals(parentPath)) {
            return storageName;
        }
        String p = parentPath.startsWith("/") ? parentPath.substring(1) : parentPath;
        if (!p.endsWith("/")) {
            p = p + "/";
        }
        return p + storageName;
    }
}
