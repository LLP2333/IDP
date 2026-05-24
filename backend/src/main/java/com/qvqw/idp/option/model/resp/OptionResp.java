package com.qvqw.idp.option.model.resp;

import com.qvqw.idp.option.OptionCategory;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 单项系统参数响应 DTO。
 *
 * <p>{@link #value} 字段已经做过 “空值回落到 defaultValue” 的处理，前端直接展示即可。</p>
 */
@Schema(description = "系统参数")
public class OptionResp {

    @Schema(description = "参数 ID", example = "1")
    private Long id;

    @Schema(description = "类别", example = "SITE")
    private OptionCategory category;

    @Schema(description = "参数名称", example = "系统名称")
    private String name;

    @Schema(description = "参数键", example = "SITE_TITLE")
    private String code;

    @Schema(description = "当前值（已回落到 defaultValue）", example = "IDP 后台管理系统")
    private String value;

    @Schema(description = "描述", example = "登录页与浏览器标题展示的系统名称")
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
