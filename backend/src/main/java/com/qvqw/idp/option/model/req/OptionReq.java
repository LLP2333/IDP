package com.qvqw.idp.option.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 参数批量修改请求项。
 *
 * <p>批量更新接口的 body 是 {@code List<OptionReq>}，每一项对应一行 {@code idp_sys_option}
 * 的更新。{@link #value} 允许为空字符串（部分配置可清空），但不允许为 {@code null}。</p>
 */
@Schema(description = "参数修改请求项")
public class OptionReq {

    @Schema(description = "参数 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "参数 ID 不能为空")
    private Long id;

    @Schema(description = "参数键", example = "SITE_TITLE", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "参数键不能为空")
    @Size(max = 100, message = "参数键长度不能超过 100")
    private String code;

    @Schema(description = "新值（允许空字符串）", example = "IDP 后台管理系统")
    private String value;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
}
