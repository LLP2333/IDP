package com.qvqw.idp.dict;

import com.qvqw.idp.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.io.Serial;

/**
 * 字典明细项。
 *
 * <p>每行代表一个字典下的可选项：</p>
 * <ul>
 *   <li>{@code value}：业务存储值（字符串形式，前后端按需 parse 为 int）；</li>
 *   <li>{@code label}：展示文案；</li>
 *   <li>{@code color}：前端 Tag 颜色（可选，约定为常见调色板名）。</li>
 * </ul>
 *
 * <p>同一字典下 {@code value} 唯一。</p>
 */
@Entity
@Table(name = "idp_sys_dict_item", uniqueConstraints = {
        @UniqueConstraint(name = "uk_idp_sys_dict_item_dict_value", columnNames = {"dict_id", "item_value"})
}, indexes = {
        @Index(name = "idx_idp_sys_dict_item_dict", columnList = "dict_id")
})
public class DictItem extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dict_id", nullable = false)
    private Long dictId;

    @Column(name = "label", nullable = false, length = 64)
    private String label;

    @Column(name = "item_value", nullable = false, length = 64)
    private String value;

    /** 可选：前端展示颜色（如 {@code primary} / {@code success} / {@code warning} / {@code danger}）。 */
    @Column(name = "color", length = 32)
    private String color;

    @Column(name = "sort", nullable = false)
    private Integer sort = 999;

    /** 1=启用, 0=禁用 */
    @Column(name = "status", nullable = false)
    private Integer status = 1;

    @Column(name = "is_system", nullable = false)
    private Boolean isSystem = false;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDictId() {
        return dictId;
    }

    public void setDictId(Long dictId) {
        this.dictId = dictId;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
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

    public Boolean getIsSystem() {
        return isSystem;
    }

    public void setIsSystem(Boolean isSystem) {
        this.isSystem = isSystem;
    }
}
