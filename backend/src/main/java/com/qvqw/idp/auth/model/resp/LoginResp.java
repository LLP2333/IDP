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
}
