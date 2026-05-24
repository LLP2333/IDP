package com.qvqw.idp.auth.internal;

import com.qvqw.idp.auth.AuthProperties;
import com.qvqw.idp.auth.UserContext;
import com.qvqw.idp.auth.UserContextHolder;
import com.qvqw.idp.auth.model.req.LoginReq;
import com.qvqw.idp.auth.model.resp.LoginResp;
import com.qvqw.idp.auth.model.resp.UserInfoResp;
import com.qvqw.idp.common.exception.BusinessException;
import com.qvqw.idp.role.RoleService;
import com.qvqw.idp.user.UserService;
import com.qvqw.idp.user.model.resp.UserDetailResp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserService userService;

    @Mock
    private RoleService roleService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenStore tokenStore;

    private JwtTokenProvider tokenProvider;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        AuthProperties props = new AuthProperties();
        props.getJwt().setSecret("test-secret-test-secret-test-secret-test-secret");
        props.getJwt().setExpires(60);
        tokenProvider = new JwtTokenProvider(props);
        authService = new AuthServiceImpl(userService, roleService, passwordEncoder, tokenProvider, tokenStore);
    }

    @AfterEach
    void clear() {
        UserContextHolder.clear();
    }

    @Test
    void loginSuccess() {
        UserService.UserCredential credential = new UserService.UserCredential(1L, "admin", "hash", 1);
        when(userService.findCredential("admin")).thenReturn(Optional.of(credential));
        when(passwordEncoder.matches("123456", "hash")).thenReturn(true);

        LoginReq req = new LoginReq();
        req.setUsername("admin");
        req.setPassword("123456");
        LoginResp resp = authService.login(req);

        assertThat(resp.getToken()).isNotBlank();
        assertThat(resp.getExpires()).isEqualTo(60);
        verify(tokenStore).put(anyString(), eq(1L), eq(60L));
    }

    @Test
    void loginWrongPassword() {
        UserService.UserCredential credential = new UserService.UserCredential(1L, "admin", "hash", 1);
        when(userService.findCredential("admin")).thenReturn(Optional.of(credential));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        LoginReq req = new LoginReq();
        req.setUsername("admin");
        req.setPassword("wrong");
        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户名或密码错误");
    }

    @Test
    void loginDisabledUser() {
        UserService.UserCredential credential = new UserService.UserCredential(1L, "admin", "hash", 0);
        when(userService.findCredential("admin")).thenReturn(Optional.of(credential));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        LoginReq req = new LoginReq();
        req.setUsername("admin");
        req.setPassword("123456");
        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("账号已被禁用");
    }

    @Test
    void getCurrentUserInfoRequiresContext() {
        assertThatThrownBy(() -> authService.getCurrentUserInfo())
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void getCurrentUserInfoOk() {
        UserContextHolder.set(new UserContext(1L, "admin", null, Set.of("admin")));
        UserDetailResp detail = new UserDetailResp();
        detail.setId(1L);
        detail.setUsername("admin");
        detail.setNickname("Administrator");
        when(userService.findById(1L)).thenReturn(Optional.of(detail));
        when(roleService.listCodesByUserId(1L)).thenReturn(Set.of("admin"));

        UserInfoResp info = authService.getCurrentUserInfo();
        assertThat(info.getUsername()).isEqualTo("admin");
        assertThat(info.getRoles()).containsExactly("admin");
        assertThat(info.getPermissions()).isEmpty();
    }
}
