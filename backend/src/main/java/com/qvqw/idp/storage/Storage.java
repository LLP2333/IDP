package com.qvqw.idp.storage;

import com.qvqw.idp.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.io.Serial;

/**
 * 存储引擎实体。
 *
 * <p>表 {@code idp_sys_storage}，承载文件管理模块依赖的多种存储引擎配置：本地文件系统、S3 协议。
 * 每条记录都有唯一编码 {@code code}，文件实体通过 {@code storageId} 引用具体的存储；
 * 全局有且只能有一条 {@code isDefault=true} 的记录，普通上传默认走它。</p>
 */
@Entity
@Table(name = "idp_sys_storage", indexes = {
        @Index(name = "uk_idp_sys_storage_code", columnList = "code", unique = true),
        @Index(name = "idx_idp_sys_storage_status", columnList = "status"),
        @Index(name = "idx_idp_sys_storage_default", columnList = "is_default")
})
public class Storage extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 显示名称。 */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /** 业务编码，全局唯一，文件实体保留。 */
    @Column(name = "code", nullable = false, length = 30)
    private String code;

    /** 存储类型，整数值，含义见 {@link StorageType#getValue()}。 */
    @Column(name = "type", nullable = false)
    private Integer type;

    /** S3 协议的 Access Key（LOCAL 类型为 null）。 */
    @Column(name = "access_key", length = 255)
    private String accessKey;

    /**
     * S3 协议的 Secret Key 密文（LOCAL 类型为 null）。
     * <p>使用 AES/GCM 落库，详见 {@link com.qvqw.idp.storage.internal.StorageSecretCipher}。</p>
     */
    @Column(name = "secret_key", length = 1024)
    private String secretKey;

    /** S3 协议的 endpoint（LOCAL 类型为 null）。 */
    @Column(name = "endpoint", length = 255)
    private String endpoint;

    /**
     * 桶名：
     * <ul>
     *   <li>LOCAL：用作绝对路径（如 {@code /var/files/}），始终以 {@code /} 结尾；</li>
     *   <li>S3：MinIO / S3 bucket 名称。</li>
     * </ul>
     */
    @Column(name = "bucket_name", length = 500)
    private String bucketName;

    /** 公开访问域名 / URL 前缀，始终以 {@code /} 结尾。 */
    @Column(name = "domain", length = 500)
    private String domain;

    /** 是否启用回收站。 */
    @Column(name = "recycle_bin_enabled", nullable = false)
    private Boolean recycleBinEnabled = Boolean.FALSE;

    /** 回收站路径（启用时非空，存储侧的相对子路径，如 {@code .RECYCLE.BIN/}）。 */
    @Column(name = "recycle_bin_path", length = 255)
    private String recycleBinPath;

    /** 描述。 */
    @Column(name = "description", length = 500)
    private String description;

    /** 是否为默认存储（全局唯一）。 */
    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = Boolean.FALSE;

    /** 排序，正整数，升序排序。 */
    @Column(name = "sort", nullable = false)
    private Integer sort = 1;

    /** 状态：1=启用，2=禁用。 */
    @Column(name = "status", nullable = false)
    private Integer status = 1;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public Boolean getRecycleBinEnabled() {
        return recycleBinEnabled;
    }

    public void setRecycleBinEnabled(Boolean recycleBinEnabled) {
        this.recycleBinEnabled = recycleBinEnabled;
    }

    public String getRecycleBinPath() {
        return recycleBinPath;
    }

    public void setRecycleBinPath(String recycleBinPath) {
        this.recycleBinPath = recycleBinPath;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    /**
     * 计算 URL 前缀：
     * <ul>
     *   <li>{@code domain} 不为空：直接使用 {@code domain}；</li>
     *   <li>否则按 {@code endpoint + bucketName} 拼接（仅 S3 类型）。</li>
     * </ul>
     *
     * @return URL 前缀（始终以 {@code /} 结尾）
     */
    public String resolveUrlPrefix() {
        if (domain != null && !domain.isBlank()) {
            return ensureTrailingSlash(domain);
        }
        if (StorageType.S3.getValue() == type) {
            String ep = ensureTrailingSlash(endpoint == null ? "" : endpoint);
            return ep + (bucketName == null ? "" : bucketName) + "/";
        }
        return "/";
    }

    private static String ensureTrailingSlash(String s) {
        if (s == null || s.isEmpty()) {
            return "/";
        }
        return s.endsWith("/") ? s : s + "/";
    }
}
