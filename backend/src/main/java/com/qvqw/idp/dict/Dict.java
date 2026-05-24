package com.qvqw.idp.dict;

import com.qvqw.idp.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.io.Serial;

/**
 * 字典实体。
 *
 * <p>字典本身只承载 “分类元信息”：编码 + 中文名 + 描述；具体可选项放在
 * {@link DictItem} 表中。前端各业务页面通过 {@code GET /system/dict/{code}/item}
 * 拉取一个字典下的全部可选项，用于下拉、Tag 颜色等渲染。</p>
 */
@Entity
@Table(name = "idp_sys_dict", uniqueConstraints = {
        @UniqueConstraint(name = "uk_idp_sys_dict_code", columnNames = "code")
})
public class Dict extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 64)
    private String name;

    @Column(name = "code", nullable = false, length = 64)
    private String code;

    @Column(name = "description", length = 255)
    private String description;

    /** 是否系统内置（内置字典禁止删除，编码不可修改）。 */
    @Column(name = "is_system", nullable = false)
    private Boolean isSystem = false;

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getIsSystem() {
        return isSystem;
    }

    public void setIsSystem(Boolean isSystem) {
        this.isSystem = isSystem;
    }
}
