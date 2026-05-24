package com.qvqw.idp.dict.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 字典新增 / 修改请求。
 */
@Schema(description = "字典新增/修改请求")
public class DictReq {

    @Schema(description = "字典名称", example = "公告分类", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "字典名称不能为空")
    @Size(max = 64, message = "字典名称长度不能超过 64")
    private String name;

    @Schema(description = "字典编码（字母开头，仅可包含字母 / 数字 / 下划线，全局唯一）",
            example = "notice_type", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "字典编码不能为空")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]{1,63}$", message = "字典编码必须以字母开头，仅可包含字母、数字、下划线")
    private String code;

    @Schema(description = "描述")
    @Size(max = 255, message = "描述长度不能超过 255")
    private String description;

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
}
