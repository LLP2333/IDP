package com.qvqw.idp.option.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 图片上传响应。
 *
 * <p>校验通过后回显图片 base64，供前端直接写回到表单字段。</p>
 */
@Schema(description = "图片上传响应")
public class OptionImageUploadResp {

    @Schema(description = "标准化后的 data URL", example = "data:image/png;base64,iVBORw0KGgo...")
    private String dataUrl;

    public OptionImageUploadResp() {
    }

    public OptionImageUploadResp(String dataUrl) {
        this.dataUrl = dataUrl;
    }

    public String getDataUrl() {
        return dataUrl;
    }

    public void setDataUrl(String dataUrl) {
        this.dataUrl = dataUrl;
    }
}
