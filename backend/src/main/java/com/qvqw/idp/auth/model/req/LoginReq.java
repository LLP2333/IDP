package com.qvqw.idp.auth.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 账号密码登录请求。
 */
@Schema(description = "登录请求")
public class LoginReq {

    @Schema(description = "用户名", example = "admin", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "用户名不能为空")
    private String username;

    @Schema(description = "密码", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "密码不能为空")
    private String password;

    @Schema(description = "验证码 ID（开启 LOGIN_CAPTCHA_ENABLED 时必传）", example = "9c3f9a2d4b8a4c8e9c3f9a2d4b8a4c8e")
    private String captchaId;

    @Schema(description = "验证码值（开启 LOGIN_CAPTCHA_ENABLED 时必传）", example = "A4F9")
    private String captcha;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getCaptchaId() {
        return captchaId;
    }

    public void setCaptchaId(String captchaId) {
        this.captchaId = captchaId;
    }

    public String getCaptcha() {
        return captcha;
    }

    public void setCaptcha(String captcha) {
        this.captcha = captcha;
    }
}
