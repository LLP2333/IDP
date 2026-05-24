package com.qvqw.idp.auth;

import com.qvqw.idp.auth.model.req.LoginReq;
import com.qvqw.idp.auth.model.resp.LoginResp;
import com.qvqw.idp.auth.model.resp.UserInfoResp;

/**
 * 认证服务（对外暴露）。
 */
public interface AuthService {

    LoginResp login(LoginReq req);

    void logout(String jti);

    UserInfoResp getCurrentUserInfo();
}
