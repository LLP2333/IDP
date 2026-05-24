package com.qvqw.idp.option.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 系统图片上传（base64）请求。
 *
 * <p>用于网站 Logo / Favicon 等以 {@code data:image/...;base64,...} 形式直接存进
 * {@link com.qvqw.idp.option.SystemOption} 的场景，无需引入文件存储模块。</p>
 */
@Schema(description = "图片上传（base64）")
public class OptionImageUploadReq {

    @Schema(description = "图片 base64（必须以 data:image/ 开头）",
            example = "data:image/png;base64,iVBORw0KGgo...",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "图片不能为空")
    private String dataUrl;

    public String getDataUrl() {
        return dataUrl;
    }

    public void setDataUrl(String dataUrl) {
        this.dataUrl = dataUrl;
    }
}
