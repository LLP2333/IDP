package com.qvqw.idp.role.internal;

import com.qvqw.idp.common.cache.AuthCacheEvictor;
import com.qvqw.idp.common.exception.BusinessException;
import com.qvqw.idp.permission.PermissionService;
import com.qvqw.idp.role.Role;
import com.qvqw.idp.role.RolePermission;
import com.qvqw.idp.role.model.req.RoleReq;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Set;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RoleServiceImplTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private RolePermissionRepository rolePermissionRepository;

    @Mock
    private PermissionService permissionService;

    @Mock
    private AuthCacheEvictor authCacheEvictor;

    @InjectMocks
    private RoleServiceImpl roleService;

    private RoleReq req;

    @BeforeEach
    void setUp() {
        req = new RoleReq();
        req.setName("测试角色");
        req.setCode("test_role");
        req.setSort(10);
        req.setStatus(1);
    }

    @Test
    void createDuplicateCodeShouldThrow() {
        when(roleRepository.existsByCode("test_role")).thenReturn(true);
        assertThatThrownBy(() -> roleService.create(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("角色编码已存在");
    }

    @Test
    void createSuccess() {
        when(roleRepository.existsByCode("test_role")).thenReturn(false);
        Role saved = new Role();
        saved.setId(99L);
        when(roleRepository.save(any(Role.class))).thenReturn(saved);

        Long id = roleService.create(req);

        assertThat(id).isEqualTo(99L);
        ArgumentCaptor<Role> captor = ArgumentCaptor.forClass(Role.class);
        verify(roleRepository).save(captor.capture());
        assertThat(captor.getValue().getCode()).isEqualTo("test_role");
        assertThat(captor.getValue().getIsSystem()).isFalse();
    }

    @Test
    void deleteSystemRoleShouldFail() {
        Role role = new Role();
        role.setId(1L);
        role.setIsSystem(true);
        role.setName("admin");
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        assertThatThrownBy(() -> roleService.delete(List.of(1L)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("系统内置角色不允许删除");
    }

    @Test
    void deleteWithBoundUserShouldFail() {
        Role role = new Role();
        role.setId(2L);
        role.setIsSystem(false);
        role.setName("test");
        when(roleRepository.findById(2L)).thenReturn(Optional.of(role));
        when(userRoleRepository.countByRoleId(2L)).thenReturn(3L);
        assertThatThrownBy(() -> roleService.delete(List.of(2L)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已分配给用户");
    }

    @Test
    void assignRolesShouldClearAndRebind() {
        when(roleRepository.findAllById(anyList())).thenReturn(List.of(new Role(), new Role()));

        roleService.assignRoles(7L, List.of(1L, 2L));

        verify(userRoleRepository).deleteByUserId(7L);
        verify(userRoleRepository, times(1)).saveAll(anyList());
    }

    @Test
    void assignRolesWithEmptyShouldOnlyClear() {
        roleService.assignRoles(7L, List.of());
        verify(userRoleRepository).deleteByUserId(7L);
        verify(userRoleRepository, times(0)).saveAll(anyList());
    }

    @Test
    void ensureRolesExistInvalidShouldThrow() {
        when(roleRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(new Role()));
        assertThatThrownBy(() -> roleService.ensureRolesExist(List.of(1L, 2L)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无效的角色 ID");
    }

    @Test
    void assignPermissionsAdminShouldFail() {
        Role admin = new Role();
        admin.setId(1L);
        admin.setCode("admin");
        when(roleRepository.findById(1L)).thenReturn(Optional.of(admin));
        assertThatThrownBy(() -> roleService.assignPermissions(1L, List.of(10L)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("admin 角色");
    }

    @Test
    void assignPermissionsInvalidIdShouldFail() {
        Role role = new Role();
        role.setId(2L);
        role.setCode("ops");
        when(roleRepository.findById(2L)).thenReturn(Optional.of(role));
        when(permissionService.listCodesByIds(List.of(10L, 20L))).thenReturn(Set.of("a"));
        assertThatThrownBy(() -> roleService.assignPermissions(2L, List.of(10L, 20L)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无效的权限");
    }

    @Test
    void assignPermissionsSuccess() {
        Role role = new Role();
        role.setId(2L);
        role.setCode("ops");
        when(roleRepository.findById(2L)).thenReturn(Optional.of(role));
        when(permissionService.listCodesByIds(List.of(10L, 20L)))
                .thenReturn(Set.of("system:user:list", "system:user:add"));
        when(userRoleRepository.findUserIdsByRoleId(2L)).thenReturn(List.of(101L, 102L));

        roleService.assignPermissions(2L, List.of(10L, 20L));

        verify(rolePermissionRepository).deleteByRoleId(2L);
        verify(rolePermissionRepository).saveAll(anyList());
        verify(authCacheEvictor).evictUsers(List.of(101L, 102L));
    }

    @Test
    void listPermissionCodesByUserIdAggregates() {
        when(userRoleRepository.findRoleIdsByUserId(7L)).thenReturn(List.of(1L, 2L));
        when(rolePermissionRepository.findPermissionIdsByRoleIds(List.of(1L, 2L)))
                .thenReturn(List.of(10L, 11L, 12L));
        when(permissionService.listCodesByIds(List.of(10L, 11L, 12L)))
                .thenReturn(Set.of("system:user:list", "system:role:list"));

        assertThat(roleService.listPermissionCodesByUserId(7L))
                .containsExactlyInAnyOrder("system:user:list", "system:role:list");
    }

    @Test
    void deleteShouldAlsoUnbindRolePermissions() {
        Role role = new Role();
        role.setId(2L);
        role.setIsSystem(false);
        role.setName("test");
        when(roleRepository.findById(2L)).thenReturn(Optional.of(role));
        when(userRoleRepository.countByRoleId(2L)).thenReturn(0L);

        roleService.delete(List.of(2L));
        verify(rolePermissionRepository).deleteByRoleId(2L);
        verify(roleRepository).deleteAllById(List.of(2L));
    }

    /** Mockito 占位以避免未使用警告。 */
    @SuppressWarnings("unused")
    private RolePermission unused;
}
