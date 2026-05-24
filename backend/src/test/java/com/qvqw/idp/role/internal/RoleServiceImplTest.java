package com.qvqw.idp.role.internal;

import com.qvqw.idp.common.cache.AuthCacheEvictor;
import com.qvqw.idp.common.exception.BusinessException;
import com.qvqw.idp.menu.MenuService;
import com.qvqw.idp.menu.model.resp.MenuResp;
import com.qvqw.idp.role.Role;
import com.qvqw.idp.role.RoleMenu;
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

import java.util.List;
import java.util.Optional;
import java.util.Set;

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
    private RoleMenuRepository roleMenuRepository;

    @Mock
    private MenuService menuService;

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
    void assignMenusAdminShouldFail() {
        Role admin = new Role();
        admin.setId(1L);
        admin.setCode("admin");
        when(roleRepository.findById(1L)).thenReturn(Optional.of(admin));
        assertThatThrownBy(() -> roleService.assignMenus(1L, List.of(10L)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("admin 角色");
    }

    @Test
    void assignMenusInvalidIdShouldFail() {
        Role role = new Role();
        role.setId(2L);
        role.setCode("ops");
        when(roleRepository.findById(2L)).thenReturn(Optional.of(role));
        // 10L 合法，20L 不存在
        MenuResp ok = new MenuResp();
        ok.setId(10L);
        when(menuService.get(10L)).thenReturn(ok);
        when(menuService.get(20L)).thenThrow(new BusinessException("菜单不存在"));
        assertThatThrownBy(() -> roleService.assignMenus(2L, List.of(10L, 20L)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无效的菜单");
    }

    @Test
    void assignMenusSuccess() {
        Role role = new Role();
        role.setId(2L);
        role.setCode("ops");
        when(roleRepository.findById(2L)).thenReturn(Optional.of(role));
        MenuResp r1 = new MenuResp();
        r1.setId(10L);
        MenuResp r2 = new MenuResp();
        r2.setId(20L);
        when(menuService.get(10L)).thenReturn(r1);
        when(menuService.get(20L)).thenReturn(r2);
        when(userRoleRepository.findUserIdsByRoleId(2L)).thenReturn(List.of(101L, 102L));

        roleService.assignMenus(2L, List.of(10L, 20L));

        verify(roleMenuRepository).deleteByRoleId(2L);
        verify(roleMenuRepository).saveAll(anyList());
        verify(authCacheEvictor).evictUsers(List.of(101L, 102L));
    }

    @Test
    void listPermissionCodesByUserIdAggregates() {
        when(userRoleRepository.findRoleIdsByUserId(7L)).thenReturn(List.of(1L, 2L));
        when(roleMenuRepository.findMenuIdsByRoleIds(List.of(1L, 2L)))
                .thenReturn(List.of(10L, 11L, 12L));
        when(menuService.listCodesByIds(List.of(10L, 11L, 12L)))
                .thenReturn(Set.of("system:user:list", "system:role:list"));

        assertThat(roleService.listPermissionCodesByUserId(7L))
                .containsExactlyInAnyOrder("system:user:list", "system:role:list");
    }

    @Test
    void deleteShouldAlsoUnbindRoleMenus() {
        Role role = new Role();
        role.setId(2L);
        role.setIsSystem(false);
        role.setName("test");
        when(roleRepository.findById(2L)).thenReturn(Optional.of(role));
        when(userRoleRepository.countByRoleId(2L)).thenReturn(0L);

        roleService.delete(List.of(2L));
        verify(roleMenuRepository).deleteByRoleId(2L);
        verify(roleRepository).deleteAllById(List.of(2L));
    }

    /** Mockito 占位以避免未使用警告。 */
    @SuppressWarnings("unused")
    private RoleMenu unused;
}
