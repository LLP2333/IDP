package com.qvqw.idp.dict.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 字典明细新增 / 修改请求。
 */
@Schema(description = "字典明细新增/修改请求")
public class DictItemReq {

    @Schema(description = "展示文案", example = "公告", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "展示文案不能为空")
    @Size(max = 64, message = "展示文案长度不能超过 64")
    private String label;

    @Schema(description = "存储值（同一字典下唯一）", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "存储值不能为空")
    @Size(max = 64, message = "存储值长度不能超过 64")
    private String value;

    @Schema(description = "Tag 颜色（前端约定）", example = "primary")
    @Size(max = 32, message = "颜色长度不能超过 32")
    private String color;

    @Schema(description = "排序值（升序）", example = "1")
    private Integer sort;

    @Schema(description = "状态：1=启用, 0=禁用", example = "1")
    private Integer status;

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
}
