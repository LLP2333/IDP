package com.qvqw.idp.option.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 公开登录配置（供登录页未登录场景调用）。
 *
 * <p>仅暴露登录页渲染所必需的开关，密码策略等不在这里返回。</p>
 */
@Schema(description = "公开登录配置")
public class LoginConfigResp {

    @Schema(description = "是否启用验证码", example = "true")
    private boolean captchaEnabled;

    public boolean isCaptchaEnabled() {
        return captchaEnabled;
    }

    public void setCaptchaEnabled(boolean captchaEnabled) {
        this.captchaEnabled = captchaEnabled;
    }
}
