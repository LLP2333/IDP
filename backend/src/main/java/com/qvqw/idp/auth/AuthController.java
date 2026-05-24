package com.qvqw.idp.auth;

import com.qvqw.idp.auth.internal.JwtTokenProvider;
import com.qvqw.idp.auth.model.req.LoginReq;
import com.qvqw.idp.auth.model.resp.LoginResp;
import com.qvqw.idp.auth.model.resp.UserInfoResp;
import com.qvqw.idp.common.api.R;
import com.qvqw.idp.common.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证 API：登录、登出、查询当前登录用户信息。
 *
 * <p>除 {@code /auth/user/info} 外，登录与登出接口不要求携带 JWT。</p>
 */
@Tag(name = "认证", description = "登录 / 登出 / 当前用户信息")
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthController(AuthService authService, JwtTokenProvider jwtTokenProvider) {
        this.authService = authService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * 账号密码登录。
     *
     * @param req 登录请求体（用户名 + 密码）
     * @return 登录成功返回 JWT 与有效期；失败抛 {@link com.qvqw.idp.common.exception.BusinessException}
     */
    @Operation(summary = "账号密码登录", description = "成功后返回 JWT 字符串与过期时间（秒）。")
    @SecurityRequirements
    @PostMapping("/login")
    public R<LoginResp> login(@RequestBody @Valid LoginReq req) {
        return R.ok(authService.login(req));
    }

    /**
     * 登出当前 JWT 对应的会话。
     *
     * <p>仅删除 Redis 中的 jti 记录，使该 token 立即失效；客户端仍需自行清理本地存储。</p>
     *
     * @param request HTTP 请求，从 {@code Authorization} 头解析 token
     * @return 始终成功
     */
    @Operation(summary = "登出", description = "解析 Authorization 头中的 JWT，将其 jti 从 Redis 中移除。")
    @SecurityRequirements
    @PostMapping("/logout")
    public R<Void> logout(HttpServletRequest request) {
        String token = resolveToken(request);
        if (StringUtils.hasText(token)) {
            try {
                JwtTokenProvider.ParsedToken parsed = jwtTokenProvider.parse(token);
                authService.logout(parsed.jti());
            } catch (Exception ignore) {
            }
        }
        return R.ok();
    }

    /**
     * 获取当前登录用户的完整信息（含角色编码、权限列表）。
     *
     * @return 用户信息；未登录时抛业务异常并由全局异常处理器转 401
     */
    @Operation(summary = "查询当前登录用户信息", description = "需在请求头中携带 Bearer JWT。")
    @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
    @GetMapping("/user/info")
    public R<UserInfoResp> getUserInfo() {
        return R.ok(authService.getCurrentUserInfo());
    }

    /**
     * 从 {@code Authorization: Bearer <token>} 中提取原始 JWT。
     *
     * @param request HTTP 请求
     * @return token 字符串；不存在或格式不正确时返回 {@code null}
     */
    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring("Bearer ".length()).trim();
        }
        return null;
    }
}
