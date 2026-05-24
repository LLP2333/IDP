package com.qvqw.idp.auth.internal;

import com.qvqw.idp.auth.AuthSessionService;
import org.springframework.stereotype.Service;

/** 认证会话服务实现。 */
@Service
public class AuthSessionServiceImpl implements AuthSessionService {

    private final TokenStore tokenStore;
    private final JwtTokenProvider tokenProvider;

    public AuthSessionServiceImpl(TokenStore tokenStore, JwtTokenProvider tokenProvider) {
        this.tokenStore = tokenStore;
        this.tokenProvider = tokenProvider;
    }

    /**
     * 判断指定 JWT ID 是否仍有效。
     *
     * <p>本方法不主动抛出 {@code BusinessException}。</p>
     */
    @Override
    public boolean existsJti(String jti) {
        return tokenStore.exists(jti);
    }

    /**
     * 解析 token 并移除对应 JWT ID，实现在线用户强退。
     *
     * <p>token 非法时由 {@link JwtTokenProvider#parse(String)} 抛出认证相关异常；
     * 本方法自身不主动抛出 {@code BusinessException}。</p>
     */
    @Override
    public void kickoutToken(String token) {
        JwtTokenProvider.ParsedToken parsed = tokenProvider.parse(token);
        tokenStore.remove(parsed.jti());
    }
}
