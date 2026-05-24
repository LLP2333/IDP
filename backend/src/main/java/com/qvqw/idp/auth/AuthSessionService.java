package com.qvqw.idp.auth;

/** 认证会话服务，供监控模块强退 / 校验 token 状态。 */
public interface AuthSessionService {

    /**
     * 判断指定 JWT ID 是否仍在服务端 TokenStore 中有效。
     *
     * @param jti JWT ID
     * @return true=有效，false=已失效或不存在
     */
    boolean existsJti(String jti);

    /**
     * 按完整 token 强制下线当前会话。
     *
     * @param token JWT 原文
     */
    void kickoutToken(String token);
}
