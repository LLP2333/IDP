package com.qvqw.idp.menu.internal;

import com.qvqw.idp.common.cache.AuthCacheEvictor;
import com.qvqw.idp.common.exception.BusinessException;
import com.qvqw.idp.menu.Menu;
import com.qvqw.idp.menu.model.req.MenuReq;
import com.qvqw.idp.menu.model.resp.MenuResp;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MenuServiceImplTest {

    @Mock
    private MenuRepository menuRepository;

    @Mock
    private AuthCacheEvictor authCacheEvictor;

    @InjectMocks
    private MenuServiceImpl service;

    @Test
    void createButtonDuplicatePermissionShouldFail() {
        Menu existing = button(99L, "system:foo:add", 5L);
        when(menuRepository.findByPermission("system:foo:add")).thenReturn(Optional.of(existing));
        when(menuRepository.findAll()).thenReturn(List.of(existing));
        when(menuRepository.findById(5L)).thenReturn(Optional.of(menu(5L, "父菜单")));

        MenuReq req = new MenuReq();
        req.setTitle("新增 Foo");
        req.setParentId(5L);
        req.setType(3);
        req.setPermission("system:foo:add");
        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("权限标识已存在");
    }

    @Test
    void createMenuWithoutComponentShouldFail() {
        when(menuRepository.findAll()).thenReturn(List.of());

        MenuReq req = new MenuReq();
        req.setTitle("用户");
        req.setParentId(0L);
        req.setType(2);
        req.setPath("/system/user");
        // missing component for type=2 non-external
        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("组件路径");
    }

    @Test
    void createExternalWithoutHttpShouldFail() {
        when(menuRepository.findAll()).thenReturn(List.of());
        MenuReq req = new MenuReq();
        req.setTitle("外链");
        req.setParentId(0L);
        req.setType(2);
        req.setPath("/system/x");
        req.setIsExternal(true);
        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("外链");
    }

    @Test
    void createDirectoryDefaultsToLayoutComponent() {
        when(menuRepository.findAll()).thenReturn(List.of());
        Menu saved = new Menu();
        saved.setId(11L);
        when(menuRepository.save(any(Menu.class))).thenReturn(saved);

        MenuReq req = new MenuReq();
        req.setTitle("系统管理");
        req.setParentId(0L);
        req.setType(1);
        req.setPath("/system");
        Long id = service.create(req);
        assertThat(id).isEqualTo(11L);
        verify(authCacheEvictor).evictAll();
    }

    @Test
    void deleteSystemBuiltinShouldFail() {
        Menu p = new Menu();
        p.setId(1L);
        p.setIsSystem(true);
        p.setTitle("用户管理");
        when(menuRepository.findById(1L)).thenReturn(Optional.of(p));
        assertThatThrownBy(() -> service.delete(List.of(1L)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("内置菜单不允许删除");
    }

    @Test
    void deleteWithChildrenShouldFail() {
        Menu p = new Menu();
        p.setId(1L);
        p.setIsSystem(false);
        p.setTitle("custom");
        when(menuRepository.findById(1L)).thenReturn(Optional.of(p));
        when(menuRepository.countByParentId(1L)).thenReturn(3L);
        assertThatThrownBy(() -> service.delete(List.of(1L)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("存在子菜单");
    }

    @Test
    void deleteOkShouldEvictCache() {
        Menu p = new Menu();
        p.setId(1L);
        p.setIsSystem(false);
        p.setTitle("custom");
        when(menuRepository.findById(1L)).thenReturn(Optional.of(p));
        when(menuRepository.countByParentId(1L)).thenReturn(0L);
        service.delete(List.of(1L));
        verify(menuRepository).deleteAllById(List.of(1L));
        verify(authCacheEvictor).evictAll();
    }

    @Test
    void updateBuiltinPermissionChangeShouldFail() {
        Menu p = button(1L, "system:user:list", 2L);
        p.setIsSystem(true);
        when(menuRepository.findById(1L)).thenReturn(Optional.of(p));
        MenuReq req = new MenuReq();
        req.setTitle("查询用户");
        req.setParentId(2L);
        req.setType(3);
        req.setPermission("system:user:list:changed");
        assertThatThrownBy(() -> service.update(1L, req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不允许修改权限标识");
    }

    @Test
    void treeShouldBuildHierarchy() {
        Menu root = dir(1L, 0L, "系统");
        Menu user = menu(2L, "用户");
        user.setParentId(1L);
        Menu list = button(3L, "system:user:list", 2L);
        when(menuRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class)))
                .thenReturn(List.of(root, user, list));

        List<MenuResp> tree = service.tree(null);
        assertThat(tree).hasSize(1);
        assertThat(tree.get(0).getChildren()).hasSize(1);
        assertThat(tree.get(0).getChildren().get(0).getChildren()).hasSize(1);
        assertThat(tree.get(0).getChildren().get(0).getChildren().get(0).getPermission())
                .isEqualTo("system:user:list");
    }

    @Test
    void treeByIdsExcludesButtons() {
        Menu root = dir(1L, 0L, "系统");
        Menu user = menu(2L, "用户");
        user.setParentId(1L);
        Menu btn = button(3L, "system:user:list", 2L);
        when(menuRepository.findByIdIn(List.of(1L, 2L, 3L))).thenReturn(List.of(root, user, btn));
        List<MenuResp> tree = service.treeByIds(List.of(1L, 2L, 3L));
        assertThat(tree).hasSize(1);
        assertThat(tree.get(0).getChildren()).hasSize(1);
        // 按钮被过滤掉
        assertThat(tree.get(0).getChildren().get(0).getChildren()).isEmpty();
    }

    @Test
    void listCodesByIdsShouldSkipDisabledOrNonButton() {
        Menu enabled = button(1L, "code1", 0L);
        enabled.setStatus(1);
        Menu disabled = button(2L, "code2", 0L);
        disabled.setStatus(0);
        Menu menuType = menu(3L, "菜单");
        menuType.setPermission("code3");
        when(menuRepository.findByIdIn(List.of(1L, 2L, 3L)))
                .thenReturn(List.of(enabled, disabled, menuType));
        Set<String> codes = service.listCodesByIds(List.of(1L, 2L, 3L));
        assertThat(codes).containsExactly("code1");
        verify(authCacheEvictor, never()).evictAll();
        verify(menuRepository, never()).save(any());
    }

    private static Menu dir(long id, long parentId, String title) {
        Menu m = new Menu();
        m.setId(id);
        m.setParentId(parentId);
        m.setTitle(title);
        m.setType(1);
        m.setSort(10);
        m.setStatus(1);
        m.setIsSystem(false);
        return m;
    }

    private static Menu menu(long id, String title) {
        Menu m = new Menu();
        m.setId(id);
        m.setParentId(0L);
        m.setTitle(title);
        m.setType(2);
        m.setPath("/" + title);
        m.setSort(10);
        m.setStatus(1);
        m.setIsSystem(false);
        return m;
    }

    private static Menu button(long id, String permission, long parentId) {
        Menu m = new Menu();
        m.setId(id);
        m.setParentId(parentId);
        m.setTitle(permission);
        m.setType(3);
        m.setPermission(permission);
        m.setSort(10);
        m.setStatus(1);
        m.setIsSystem(false);
        return m;
    }
}
