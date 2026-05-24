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
        Set<String> codes = roleService.listCodesByUserId(ctx.getId());
        resp.setRoles(codes.stream().toList());
        resp.setPermissions(List.of());
        return resp;
    }
}
