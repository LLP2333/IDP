package com.qvqw.idp.auth.model.resp;

/**
 * 登录响应。
 */
public class LoginResp {

    private String token;
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
