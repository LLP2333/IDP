package com.qvqw.idp.user.internal;

import com.qvqw.idp.common.cache.AuthCacheEvictor;
import com.qvqw.idp.common.exception.BusinessException;
import com.qvqw.idp.option.OptionService;
import com.qvqw.idp.role.RoleService;
import com.qvqw.idp.user.User;
import com.qvqw.idp.user.UserPasswordHistory;
import com.qvqw.idp.user.UserService;
import com.qvqw.idp.user.model.req.UserBasicInfoUpdateReq;
import com.qvqw.idp.user.model.req.UserCreateReq;
import com.qvqw.idp.user.model.req.UserPasswordChangeReq;
import com.qvqw.idp.user.model.req.UserPasswordResetReq;
import com.qvqw.idp.user.model.req.UserRoleUpdateReq;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleService roleService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserPasswordHistoryRepository passwordHistoryRepository;

    @Mock
    private PasswordValidator passwordValidator;

    @Mock
    private OptionService optionService;

    @Mock
    private AuthCacheEvictor authCacheEvictor;

    @InjectMocks
    private UserServiceImpl userService;

    private UserCreateReq createReq;

    @BeforeEach
    void setUp() {
        createReq = new UserCreateReq();
        createReq.setUsername("zhangsan");
        createReq.setPassword("123456");
        createReq.setNickname("张三");
        createReq.setRoleIds(List.of(2L));
    }

    @Test
    void createDuplicateUsernameShouldThrow() {
        when(userRepository.existsByUsername("zhangsan")).thenReturn(true);
        assertThatThrownBy(() -> userService.create(createReq))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户名已存在");
    }

    @Test
    void createSuccessShouldAssignRoles() {
        when(userRepository.existsByUsername("zhangsan")).thenReturn(false);
        when(passwordEncoder.encode("123456")).thenReturn("hash");
        User saved = new User();
        saved.setId(10L);
        when(userRepository.save(any(User.class))).thenReturn(saved);

        Long id = userService.create(createReq);

        assertThat(id).isEqualTo(10L);
        verify(roleService).ensureRolesExist(List.of(2L));
        verify(roleService).assignRoles(10L, List.of(2L));
    }

    @Test
    void resetPasswordShouldUpdateHash() {
        User user = new User();
        user.setId(5L);
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newpwd")).thenReturn("newhash");

        UserPasswordResetReq req = new UserPasswordResetReq();
        req.setNewPassword("newpwd");
        userService.resetPassword(5L, req);

        assertThat(user.getPassword()).isEqualTo("newhash");
        assertThat(user.getPwdResetAt()).isNotNull();
        verify(userRepository).save(user);
    }

    @Test
    void deleteSystemUserShouldFail() {
        User u = new User();
        u.setId(1L);
        u.setUsername("admin");
        u.setIsSystem(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));
        assertThatThrownBy(() -> userService.delete(List.of(1L)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("系统内置用户不允许删除");
    }

    @Test
    void updateRoleDelegatesToRoleService() {
        when(userRepository.existsById(3L)).thenReturn(true);
        UserRoleUpdateReq req = new UserRoleUpdateReq();
        req.setRoleIds(List.of(1L, 2L));
        userService.updateRole(3L, req);
        verify(roleService).assignRoles(3L, List.of(1L, 2L));
    }

    @Test
    void findCredentialReturnsHash() {
        User user = new User();
        user.setId(1L);
        user.setUsername("admin");
        user.setPassword("hash");
        user.setStatus(1);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));

        Optional<UserService.UserCredential> credential = userService.findCredential("admin");
        assertThat(credential).isPresent();
        assertThat(credential.get().passwordHash()).isEqualTo("hash");
    }

    @Test
    void changeCurrentPasswordWrongOldShouldFail() {
        User user = new User();
        user.setId(1L);
        user.setUsername("admin");
        user.setPassword("hash");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(eq("wrong"), eq("hash"))).thenReturn(false);

        UserPasswordChangeReq req = new UserPasswordChangeReq();
        req.setOldPassword("wrong");
        req.setNewPassword("Newpwd#234");
        assertThatThrownBy(() -> userService.changeCurrentPassword(1L, req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("原密码错误");
    }

    @Test
    void changeCurrentPasswordSameAsOldShouldFail() {
        User user = new User();
        user.setId(1L);
        user.setUsername("admin");
        user.setPassword("hash");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(eq("samepwd"), eq("hash"))).thenReturn(true);

        UserPasswordChangeReq req = new UserPasswordChangeReq();
        req.setOldPassword("samepwd");
        req.setNewPassword("samepwd");
        assertThatThrownBy(() -> userService.changeCurrentPassword(1L, req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能与原密码相同");
    }

    @Test
    void changeCurrentPasswordOk() {
        User user = new User();
        user.setId(1L);
        user.setUsername("admin");
        user.setPassword("hash");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(eq("OldPass#123"), eq("hash"))).thenReturn(true);
        when(passwordEncoder.encode("NewPass#234")).thenReturn("newhash");
        when(optionService.getIntOrDefault(anyString(), anyInt())).thenReturn(3);

        UserPasswordChangeReq req = new UserPasswordChangeReq();
        req.setOldPassword("OldPass#123");
        req.setNewPassword("NewPass#234");
        userService.changeCurrentPassword(1L, req);

        verify(passwordValidator).validate(eq("NewPass#234"), eq("admin"), anyList());
        verify(passwordHistoryRepository).save(any(UserPasswordHistory.class));
        assertThat(user.getPassword()).isEqualTo("newhash");
    }

    @Test
    void updateCurrentUserBasicInfoShouldUpdateOnlyProvidedFields() {
        User user = new User();
        user.setId(1L);
        user.setUsername("admin");
        user.setNickname("旧昵称");
        user.setEmail("old@x.com");
        user.setPhone("13800000000");
        user.setGender(0);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserBasicInfoUpdateReq req = new UserBasicInfoUpdateReq();
        req.setNickname("新昵称");
        req.setGender(1);

        userService.updateCurrentUserBasicInfo(1L, req);

        assertThat(user.getNickname()).isEqualTo("新昵称");
        assertThat(user.getGender()).isEqualTo(1);
        // 未传字段保持原值
        assertThat(user.getEmail()).isEqualTo("old@x.com");
        assertThat(user.getPhone()).isEqualTo("13800000000");
        verify(userRepository).save(user);
    }

    @Test
    void updateCurrentUserBasicInfoEmptyEmailShouldClear() {
        User user = new User();
        user.setId(1L);
        user.setUsername("admin");
        user.setEmail("old@x.com");
        user.setPhone("13800000000");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserBasicInfoUpdateReq req = new UserBasicInfoUpdateReq();
        req.setEmail("");
        req.setPhone("");

        userService.updateCurrentUserBasicInfo(1L, req);

        assertThat(user.getEmail()).isNull();
        assertThat(user.getPhone()).isNull();
    }

    @Test
    void updateCurrentUserBasicInfoBlankNicknameShouldThrow() {
        User user = new User();
        user.setId(1L);
        user.setNickname("张三");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserBasicInfoUpdateReq req = new UserBasicInfoUpdateReq();
        req.setNickname("   ");

        assertThatThrownBy(() -> userService.updateCurrentUserBasicInfo(1L, req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("昵称不能为空");
    }

    @Test
    void updateCurrentUserBasicInfoIllegalGenderShouldThrow() {
        User user = new User();
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserBasicInfoUpdateReq req = new UserBasicInfoUpdateReq();
        req.setGender(99);

        assertThatThrownBy(() -> userService.updateCurrentUserBasicInfo(1L, req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("性别");
    }

    @Test
    void updateCurrentUserBasicInfoUserNotFoundShouldThrow() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        UserBasicInfoUpdateReq req = new UserBasicInfoUpdateReq();
        req.setNickname("x");
        assertThatThrownBy(() -> userService.updateCurrentUserBasicInfo(99L, req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户不存在");
    }

    @Test
    void increasePwdErrorCountShouldLockWhenReachThreshold() {
        User user = new User();
        user.setId(1L);
        user.setPwdErrorCount(4);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        int after = userService.increasePwdErrorCount(1L, 5, java.time.LocalDateTime.now().plusMinutes(5));
        assertThat(after).isEqualTo(5);
        assertThat(user.getPwdLockedUntil()).isNotNull();
    }
}
