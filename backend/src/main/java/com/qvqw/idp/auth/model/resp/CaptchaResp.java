package com.qvqw.idp.auth.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 验证码生成响应。
 */
@Schema(description = "验证码")
public class CaptchaResp {

    @Schema(description = "验证码 ID，登录时回传", example = "9c3f9a2d4b8a4c8e9c3f9a2d4b8a4c8e")
    private String captchaId;

    @Schema(description = "图片 dataUrl（SVG base64）",
            example = "data:image/svg+xml;base64,PHN2Zy...")
    private String image;

    public CaptchaResp() {
    }

    public CaptchaResp(String captchaId, String image) {
        this.captchaId = captchaId;
        this.image = image;
    }

    public String getCaptchaId() {
        return captchaId;
    }

    public void setCaptchaId(String captchaId) {
        this.captchaId = captchaId;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }
}
