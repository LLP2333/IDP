package com.qvqw.idp.auth.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 登录响应。
 */
@Schema(description = "登录响应")
public class LoginResp {

    @Schema(description = "JWT token，需放在后续请求的 Authorization 头中", example = "eyJhbGciOiJIUzI1...")
    private String token;

    @Schema(description = "token 有效期（秒）", example = "3600")
    private long expires;

    @Schema(description = "密码已过期；前端应立即弹出强制改密 Modal", example = "false")
    private boolean passwordExpired;

    @Schema(description = "密码即将过期（剩余天数 ≤ 警告天数）", example = "false")
    private boolean passwordWarning;

    @Schema(description = "密码剩余天数；为 0 表示永不过期或已过期", example = "30")
    private Long passwordExpiresInDays;

    public LoginResp() {
    }

    public LoginResp(String token, long expires) {
        this.token = token;
        this.expires = expires;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public long getExpires() {
        return expires;
    }

    public void setExpires(long expires) {
        this.expires = expires;
    }

    public boolean isPasswordExpired() {
        return passwordExpired;
    }

    public void setPasswordExpired(boolean passwordExpired) {
        this.passwordExpired = passwordExpired;
    }

    public boolean isPasswordWarning() {
        return passwordWarning;
    }

    public void setPasswordWarning(boolean passwordWarning) {
        this.passwordWarning = passwordWarning;
    }

    public Long getPasswordExpiresInDays() {
        return passwordExpiresInDays;
    }

    public void setPasswordExpiresInDays(Long passwordExpiresInDays) {
        this.passwordExpiresInDays = passwordExpiresInDays;
    }
}
