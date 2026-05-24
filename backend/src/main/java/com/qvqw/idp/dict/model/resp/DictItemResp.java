package com.qvqw.idp.dict.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 字典明细响应 DTO。
 *
 * <p>前端各下拉 / Tag 渲染均依赖 {@code label} + {@code value}（+ 可选 {@code color}）。</p>
 */
@Schema(description = "字典明细")
public class DictItemResp {

    @Schema(description = "明细 ID", example = "1")
    private Long id;

    @Schema(description = "所属字典 ID", example = "1")
    private Long dictId;

    @Schema(description = "展示文案", example = "公告")
    private String label;

    @Schema(description = "存储值", example = "1")
    private String value;

    @Schema(description = "Tag 颜色（可空）", example = "primary")
    private String color;

    @Schema(description = "排序值（升序）", example = "1")
    private Integer sort;

    @Schema(description = "状态：1=启用, 0=禁用", example = "1")
    private Integer status;

    @Schema(description = "是否系统内置")
    private Boolean isSystem;

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
