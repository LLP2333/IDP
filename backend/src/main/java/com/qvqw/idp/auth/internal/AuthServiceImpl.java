package com.qvqw.idp.auth.internal;

import com.qvqw.idp.auth.AuthService;
import com.qvqw.idp.auth.model.req.LoginReq;
import com.qvqw.idp.auth.model.resp.LoginResp;
import com.qvqw.idp.auth.model.resp.UserInfoResp;
import com.qvqw.idp.common.exception.BusinessException;
import com.qvqw.idp.common.security.UserContext;
import com.qvqw.idp.common.security.UserContextHolder;
import com.qvqw.idp.option.OptionService;
import com.qvqw.idp.option.PasswordPolicy;
import com.qvqw.idp.role.RoleService;
import com.qvqw.idp.user.UserService;
import com.qvqw.idp.user.model.resp.UserDetailResp;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * 认证服务实现：完成账号密码校验、签发 / 注销 JWT 与查询当前登录用户信息。
 *
 * <p>登录链路按以下顺序处理：</p>
 * <ol>
 *   <li>如果 {@code LOGIN_CAPTCHA_ENABLED=1}，先消耗一次验证码（错误立即失败）；</li>
 *   <li>查找用户凭证，不存在 / 密码错误统一返回 “用户名或密码错误”；</li>
 *   <li>检查 {@code pwd_locked_until} 是否仍在锁定窗口内；</li>
 *   <li>校验账号是否被禁用；</li>
 *   <li>密码错误时累加 {@code pwd_error_count}，达到 {@code PASSWORD_ERROR_LOCK_COUNT}
 *       则同步写入 {@code pwd_locked_until}；</li>
 *   <li>成功时清零失败计数 + 解锁，并根据 {@code PASSWORD_EXPIRATION_DAYS}
 *       与 {@code PASSWORD_EXPIRATION_WARNING_DAYS} 计算到期状态，回写到响应。</li>
 * </ol>
 */
@Service
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final RoleService roleService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final TokenStore tokenStore;
    private final OptionService optionService;
    private final CaptchaService captchaService;

    public AuthServiceImpl(UserService userService,
                           RoleService roleService,
                           PasswordEncoder passwordEncoder,
                           JwtTokenProvider tokenProvider,
                           TokenStore tokenStore,
                           OptionService optionService,
                           CaptchaService captchaService) {
        this.userService = userService;
        this.roleService = roleService;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.tokenStore = tokenStore;
        this.optionService = optionService;
        this.captchaService = captchaService;
    }

    @Override
    public LoginResp login(LoginReq req) {
        boolean captchaEnabled = optionService.getIntOrDefault("LOGIN_CAPTCHA_ENABLED", 0) == 1;
        if (captchaEnabled) {
            captchaService.consume(req.getCaptchaId(), req.getCaptcha());
        }
        UserService.UserCredential credential = userService.findCredential(req.getUsername())
                .orElseThrow(() -> new BusinessException("用户名或密码错误"));
        // 锁定窗口内直接拒绝
        if (credential.pwdLockedUntil() != null && credential.pwdLockedUntil().isAfter(LocalDateTime.now())) {
            long minutes = Math.max(1, Duration.between(LocalDateTime.now(), credential.pwdLockedUntil()).toMinutes());
            throw new BusinessException("账号已锁定，请 %d 分钟后再试".formatted(minutes));
        }
        if (credential.status() == null || credential.status() != 1) {
            throw new BusinessException("账号已被禁用");
        }
        if (!passwordEncoder.matches(req.getPassword(), credential.passwordHash())) {
            int maxCount = optionService.getIntOrDefault(PasswordPolicy.PASSWORD_ERROR_LOCK_COUNT.name(), 0);
            int lockMinutes = optionService.getIntOrDefault(PasswordPolicy.PASSWORD_ERROR_LOCK_MINUTES.name(), 5);
            LocalDateTime lockUntil = (maxCount > 0)
                    ? LocalDateTime.now().plusMinutes(Math.max(1, lockMinutes))
                    : null;
            int current = userService.increasePwdErrorCount(credential.id(), maxCount, lockUntil);
            if (maxCount > 0 && current >= maxCount) {
                throw new BusinessException("密码错误次数过多，账号已锁定 %d 分钟".formatted(Math.max(1, lockMinutes)));
            }
            throw new BusinessException("用户名或密码错误");
        }
        // 成功：清零失败计数 + 解锁
        userService.resetPwdErrorAndUnlock(credential.id());
        JwtTokenProvider.IssuedToken issued = tokenProvider.issue(credential.id(), credential.username());
        tokenStore.put(issued.jti(), issued.userId(), tokenProvider.getExpires());
        LoginResp resp = new LoginResp(issued.token(), tokenProvider.getExpires());
        // 密码到期计算
        int expirationDays = optionService.getIntOrDefault(PasswordPolicy.PASSWORD_EXPIRATION_DAYS.name(), 0);
        int warningDays = optionService.getIntOrDefault(PasswordPolicy.PASSWORD_EXPIRATION_WARNING_DAYS.name(), 0);
        if (expirationDays > 0 && credential.pwdResetAt() != null) {
            long daysSince = Duration.between(credential.pwdResetAt(), LocalDateTime.now()).toDays();
            long remaining = expirationDays - daysSince;
            resp.setPasswordExpiresInDays(Math.max(0, remaining));
            if (remaining <= 0) {
                resp.setPasswordExpired(true);
            } else if (warningDays > 0 && remaining <= warningDays) {
                resp.setPasswordWarning(true);
            }
        }
        return resp;
    }

    @Override
    public void logout(String jti) {
        if (jti != null) {
            tokenStore.remove(jti);
        }
    }

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
        Set<String> roleCodes = roleService.listCodesByUserId(ctx.getId());
        resp.setRoles(roleCodes.stream().sorted().toList());
        Set<String> permCodes = roleService.listPermissionCodesByUserId(ctx.getId());
        resp.setPermissions(permCodes.stream().sorted().toList());
        return resp;
    }
}
