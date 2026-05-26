package com.qvqw.idp.storage;

/**
 * 存储引擎类型枚举。
 *
 * <ul>
 *   <li>{@link #LOCAL}：本地文件系统存储，{@code bucketName} 字段保存绝对路径，{@code domain} 字段保存公开访问 URL 前缀；</li>
 *   <li>{@link #S3}：S3 协议存储（AWS S3 / MinIO / 阿里 OSS / 腾讯 COS 等），需要 accessKey/secretKey/endpoint/bucketName。</li>
 * </ul>
 */
public enum StorageType {

    /** 本地文件系统存储。 */
    LOCAL(1),

    /** S3 协议存储（MinIO / AWS S3 / 兼容服务）。 */
    S3(2);

    private final int value;

    StorageType(int value) {
        this.value = value;
    }

    /**
     * @return 数据库与前端协议中使用的整型值
     */
    public int getValue() {
        return value;
    }

    /**
     * 按整型值反查枚举。
     *
     * @param value 数据库中的整型值
     * @return 对应的枚举；未知值返回 {@code null}
     */
    public static StorageType ofValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (StorageType t : values()) {
            if (t.value == value) {
                return t;
            }
        }
        return null;
    }
}
