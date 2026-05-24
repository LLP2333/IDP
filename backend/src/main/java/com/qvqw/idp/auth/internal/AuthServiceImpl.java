package com.qvqw.idp.auth.internal;

import com.qvqw.idp.auth.AuthService;
import com.qvqw.idp.auth.UserContext;
import com.qvqw.idp.auth.UserContextHolder;
import com.qvqw.idp.auth.model.req.LoginReq;
import com.qvqw.idp.auth.model.resp.LoginResp;
import com.qvqw.idp.auth.model.resp.UserInfoResp;
import com.qvqw.idp.common.exception.BusinessException;
import com.qvqw.idp.role.RoleService;
import com.qvqw.idp.user.UserService;
import com.qvqw.idp.user.model.resp.UserDetailResp;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * 认证服务实现：完成账号密码校验、签发 / 注销 JWT 与查询当前登录用户信息。
 */
@Service
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final RoleService roleService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final TokenStore tokenStore;

    public AuthServiceImpl(UserService userService,
                           RoleService roleService,
                           PasswordEncoder passwordEncoder,
                           JwtTokenProvider tokenProvider,
                           TokenStore tokenStore) {
        this.userService = userService;
        this.roleService = roleService;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.tokenStore = tokenStore;
    }

    /**
     * 校验账号密码并签发 JWT。
     *
     * <p>错误场景统一抛出 {@link BusinessException} 并由全局异常处理器返回 400，避免暴露
     * “用户名错误” / “密码错误” 之类的差异，降低被枚举撞库的风险。</p>
     *
     * @param req 登录请求
     * @return 含 token 与过期秒数的登录响应
     * @throws BusinessException 用户名或密码错误、账号被禁用
     */
    @Override
    public LoginResp login(LoginReq req) {
        UserService.UserCredential credential = userService.findCredential(req.getUsername())
                .orElseThrow(() -> new BusinessException("用户名或密码错误"));
        if (!passwordEncoder.matches(req.getPassword(), credential.passwordHash())) {
            throw new BusinessException("用户名或密码错误");
        }
        if (credential.status() == null || credential.status() != 1) {
            throw new BusinessException("账号已被禁用");
        }
        JwtTokenProvider.IssuedToken issued = tokenProvider.issue(credential.id(), credential.username());
        tokenStore.put(issued.jti(), issued.userId(), tokenProvider.getExpires());
        return new LoginResp(issued.token(), tokenProvider.getExpires());
    }

    /**
     * 注销指定 jti（JWT ID）的会话。
     *
     * <p>仅从 Redis 中删除 jti 即可，剩余生命周期由客户端自行清理。{@code jti} 为 {@code null}
     * 时本方法无副作用。</p>
     *
     * @param jti 待失效的 JWT ID
     */
    @Override
    public void logout(String jti) {
        if (jti != null) {
            tokenStore.remove(jti);
        }
    }

    /**
     * 读取当前线程上下文中的用户，组装其昵称 / 头像 / 角色编码等完整信息。
     *
     * @return 用户信息
     * @throws BusinessException 未登录（{@code code=401}）或用户已被删除
     */
    @Override
    public UserInfoResp getCurrentUserInfo() {
        UserContext ctx = UserContextHolder.get();
        if (ctx == null) {
            throw new BusinessException(401, "未登录");
        }
        UserDetailResp detail = userService.findById(ctx.getId())
                .orElseThrow(() -> new BusinessException("用户不存在"));
        UserInfoResp resp = new UserInfoResp();
        resp.setId(detail.getId());
        resp.setUsername(detail.getUsername());
        resp.setNickname(detail.getNickname());
        resp.setAvatar(detail.getAvatar());
        resp.setEmail(detail.getEmail());
        resp.setPhone(detail.getPhone());
        Set<String> codes = roleService.listCodesByUserId(ctx.getId());
        resp.setRoles(codes.stream().toList());
        resp.setPermissions(List.of());
        return resp;
    }
}
