package com.qvqw.idp.option;

import com.qvqw.idp.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.io.Serial;

/**
 * 系统参数实体（表 {@code idp_sys_option}）。
 *
 * <p>{@link #value} 为空时业务侧应回落到 {@link #defaultValue}；{@code code} 在同一
 * {@link #category} 下唯一。这里只放 “通用键值对”，业务取值与校验逻辑落在 {@link OptionService}
 * 以及 {@link PasswordPolicy} 等领域枚举里，避免实体承担业务规则。</p>
 */
@Entity
@Table(name = "idp_sys_option", uniqueConstraints = {
        @UniqueConstraint(name = "uk_idp_sys_option_category_code", columnNames = {"category", "code"})
})
public class SystemOption extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 类别：SITE / PASSWORD / LOGIN。 */
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 32)
    private OptionCategory category;

    /** 配置名称（前端表单 label 与提示展示使用）。 */
    @Column(name = "name", nullable = false, length = 64)
    private String name;

    /** 业务侧引用的键，全局规范为 {@code 大写 + 下划线}（如 {@code SITE_TITLE}）。 */
    @Column(name = "code", nullable = false, length = 100)
    private String code;

    /** 当前值。允许为空（图片等字段可能未设置）。列名 {@code option_value}，避免与 H2 / 部分方言保留字 {@code value} 冲突。 */
    @Column(name = "option_value", columnDefinition = "text")
    private String value;

    /** 默认值。{@code value} 为空时回落到此字段。 */
    @Column(name = "default_value", columnDefinition = "text")
    private String defaultValue;

    /** 描述（前端表单 help 文案）。 */
    @Column(name = "description", length = 255)
    private String description;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public OptionCategory getCategory() {
        return category;
    }

    public void setCategory(OptionCategory category) {
        this.category = category;
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

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * 返回当前实际值。{@code value} 非空时直接返回，否则返回 {@code defaultValue}。
     *
     * @return 实际值，可能为 {@code null}（当 value 与 defaultValue 都为空时）
     */
    public String effectiveValue() {
        return (value == null || value.isEmpty()) ? defaultValue : value;
    }
}
