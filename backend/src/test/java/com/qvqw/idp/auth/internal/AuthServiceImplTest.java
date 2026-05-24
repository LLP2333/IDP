package com.qvqw.idp.auth.internal;

import com.qvqw.idp.auth.AuthProperties;
import com.qvqw.idp.auth.model.req.LoginReq;
import com.qvqw.idp.auth.model.resp.LoginResp;
import com.qvqw.idp.auth.model.resp.UserInfoResp;
import com.qvqw.idp.common.exception.BusinessException;
import com.qvqw.idp.common.security.UserContext;
import com.qvqw.idp.common.security.UserContextHolder;
import com.qvqw.idp.menu.MenuService;
import com.qvqw.idp.menu.model.resp.MenuResp;
import com.qvqw.idp.option.OptionService;
import com.qvqw.idp.role.RoleService;
import com.qvqw.idp.user.UserService;
import com.qvqw.idp.user.model.resp.UserDetailResp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceImplTest {

    @Mock
    private UserService userService;

    @Mock
    private RoleService roleService;

    @Mock
    private MenuService menuService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenStore tokenStore;

    @Mock
    private OptionService optionService;

    @Mock
    private CaptchaService captchaService;

    private JwtTokenProvider tokenProvider;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        AuthProperties props = new AuthProperties();
        props.getJwt().setSecret("test-secret-test-secret-test-secret-test-secret");
        props.getJwt().setExpires(60);
        tokenProvider = new JwtTokenProvider(props);
        authService = new AuthServiceImpl(userService, roleService, menuService, passwordEncoder,
                tokenProvider, tokenStore, optionService, captchaService);
        // 默认关闭验证码，便于现有 case 简化
        when(optionService.getIntOrDefault(eq("LOGIN_CAPTCHA_ENABLED"), anyInt())).thenReturn(0);
        when(optionService.getIntOrDefault(anyString(), anyInt())).thenReturn(0);
    }

    @AfterEach
    void clear() {
        UserContextHolder.clear();
    }

    @Test
    void loginSuccess() {
        UserService.UserCredential credential = new UserService.UserCredential(
                1L, "admin", "hash", 1, LocalDateTime.now(), 0, null);
        when(userService.findCredential("admin")).thenReturn(Optional.of(credential));
        when(passwordEncoder.matches("123456", "hash")).thenReturn(true);

        LoginReq req = new LoginReq();
        req.setUsername("admin");
        req.setPassword("123456");
        LoginResp resp = authService.login(req);

        assertThat(resp.getToken()).isNotBlank();
        assertThat(resp.getExpires()).isEqualTo(60);
        verify(tokenStore).put(anyString(), eq(1L), eq(60L));
        verify(userService).resetPwdErrorAndUnlock(1L);
    }

    @Test
    void loginWrongPassword() {
        UserService.UserCredential credential = new UserService.UserCredential(
                1L, "admin", "hash", 1, LocalDateTime.now(), 0, null);
        when(userService.findCredential("admin")).thenReturn(Optional.of(credential));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        LoginReq req = new LoginReq();
        req.setUsername("admin");
        req.setPassword("wrong");
        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户名或密码错误");
        verify(userService).increasePwdErrorCount(eq(1L), anyInt(), any());
    }

    @Test
    void loginDisabledUser() {
        UserService.UserCredential credential = new UserService.UserCredential(
                1L, "admin", "hash", 0, LocalDateTime.now(), 0, null);
        when(userService.findCredential("admin")).thenReturn(Optional.of(credential));

        LoginReq req = new LoginReq();
        req.setUsername("admin");
        req.setPassword("123456");
        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("账号已被禁用");
    }

    @Test
    void loginLockedUser() {
        UserService.UserCredential credential = new UserService.UserCredential(
                1L, "admin", "hash", 1, LocalDateTime.now(), 5, LocalDateTime.now().plusMinutes(5));
        when(userService.findCredential("admin")).thenReturn(Optional.of(credential));

        LoginReq req = new LoginReq();
        req.setUsername("admin");
        req.setPassword("123456");
        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("账号已锁定");
    }

    @Test
    void loginWithCaptchaEnabledShouldConsumeCaptcha() {
        when(optionService.getIntOrDefault(eq("LOGIN_CAPTCHA_ENABLED"), anyInt())).thenReturn(1);
        UserService.UserCredential credential = new UserService.UserCredential(
                1L, "admin", "hash", 1, LocalDateTime.now(), 0, null);
        when(userService.findCredential("admin")).thenReturn(Optional.of(credential));
        when(passwordEncoder.matches("123456", "hash")).thenReturn(true);

        LoginReq req = new LoginReq();
        req.setUsername("admin");
        req.setPassword("123456");
        req.setCaptchaId("cid");
        req.setCaptcha("ABCD");
        authService.login(req);
        verify(captchaService).consume("cid", "ABCD");
    }

    @Test
    void getCurrentUserInfoRequiresContext() {
        assertThatThrownBy(() -> authService.getCurrentUserInfo())
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void getCurrentUserInfoOk() {
        UserContextHolder.set(new UserContext(1L, "admin", null, Set.of("admin"), Set.of("system:user:list")));
        UserDetailResp detail = new UserDetailResp();
        detail.setId(1L);
        detail.setUsername("admin");
        detail.setNickname("Administrator");
        when(userService.findById(1L)).thenReturn(Optional.of(detail));
        when(roleService.listCodesByUserId(1L)).thenReturn(Set.of("admin"));
        when(roleService.listPermissionCodesByUserId(1L)).thenReturn(Set.of("system:user:list", "system:user:add"));

        UserInfoResp info = authService.getCurrentUserInfo();
        assertThat(info.getUsername()).isEqualTo("admin");
        assertThat(info.getRoles()).containsExactly("admin");
        assertThat(info.getPermissions()).containsExactly("system:user:add", "system:user:list");
    }

    @Test
    void getCurrentUserRouteAdminReturnsAllEnabled() {
        UserContextHolder.set(new UserContext(1L, "admin", null, Set.of("admin"), Set.of()));
        MenuResp m = new MenuResp();
        m.setId(1L);
        m.setTitle("系统管理");
        m.setType(1);
        when(menuService.treeAllEnabledRoutes()).thenReturn(List.of(m));

        List<MenuResp> route = authService.getCurrentUserRoute();
        assertThat(route).hasSize(1);
        assertThat(route.get(0).getTitle()).isEqualTo("系统管理");
    }

    @Test
    void getCurrentUserRouteNormalUserAggregatesMenus() {
        UserContextHolder.set(new UserContext(7L, "ops", null, Set.of("ops"), Set.of("system:user:list")));
        when(roleService.listRoleIdsByUserId(7L)).thenReturn(List.of(2L, 3L));
        when(roleService.listMenuIdsByRoleId(2L)).thenReturn(List.of(10L, 11L));
        when(roleService.listMenuIdsByRoleId(3L)).thenReturn(List.of(11L, 12L));
        MenuResp m = new MenuResp();
        m.setId(10L);
        when(menuService.treeByIds(org.mockito.ArgumentMatchers.argThat(
                set -> set != null && set.containsAll(java.util.Set.of(10L, 11L, 12L)))))
                .thenReturn(List.of(m));

        List<MenuResp> route = authService.getCurrentUserRoute();
        assertThat(route).hasSize(1);
    }

    @Test
    void getCurrentUserRouteNoLoginShouldThrow() {
        assertThatThrownBy(() -> authService.getCurrentUserRoute())
                .isInstanceOf(BusinessException.class);
    }
}
