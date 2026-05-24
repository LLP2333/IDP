package com.qvqw.idp.permission.internal;

import com.qvqw.idp.common.cache.AuthCacheEvictor;
import com.qvqw.idp.common.exception.BusinessException;
import com.qvqw.idp.permission.Permission;
import com.qvqw.idp.permission.model.req.PermissionReq;
import com.qvqw.idp.permission.model.resp.PermissionResp;
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
class PermissionServiceImplTest {

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private AuthCacheEvictor authCacheEvictor;

    @InjectMocks
    private PermissionServiceImpl service;

    @Test
    void createDuplicateCodeShouldFail() {
        when(permissionRepository.existsByCode("system:foo")).thenReturn(true);
        PermissionReq req = new PermissionReq();
        req.setCode("system:foo");
        req.setName("foo");
        req.setParentId(0L);
        req.setType(2);
        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("编码已存在");
    }

    @Test
    void deleteSystemBuiltinShouldFail() {
        Permission p = new Permission();
        p.setId(1L);
        p.setIsSystem(true);
        p.setCode("system:user:list");
        when(permissionRepository.findById(1L)).thenReturn(Optional.of(p));
        assertThatThrownBy(() -> service.delete(List.of(1L)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("内置权限不允许删除");
    }

    @Test
    void deleteWithChildrenShouldFail() {
        Permission p = new Permission();
        p.setId(1L);
        p.setIsSystem(false);
        p.setCode("custom");
        when(permissionRepository.findById(1L)).thenReturn(Optional.of(p));
        when(permissionRepository.countByParentId(1L)).thenReturn(3L);
        assertThatThrownBy(() -> service.delete(List.of(1L)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("存在子节点");
    }

    @Test
    void deleteOkShouldEvictCache() {
        Permission p = new Permission();
        p.setId(1L);
        p.setIsSystem(false);
        p.setCode("custom");
        when(permissionRepository.findById(1L)).thenReturn(Optional.of(p));
        when(permissionRepository.countByParentId(1L)).thenReturn(0L);
        service.delete(List.of(1L));
        verify(permissionRepository).deleteAllById(List.of(1L));
        verify(authCacheEvictor).evictAll();
    }

    @Test
    void updateBuiltinCodeChangeShouldFail() {
        Permission p = new Permission();
        p.setId(1L);
        p.setIsSystem(true);
        p.setCode("system:user:list");
        p.setParentId(2L);
        when(permissionRepository.findById(1L)).thenReturn(Optional.of(p));
        PermissionReq req = new PermissionReq();
        req.setCode("system:user:list:changed");
        req.setName("name");
        req.setParentId(2L);
        req.setType(2);
        assertThatThrownBy(() -> service.update(1L, req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不允许修改编码");
    }

    @Test
    void treeShouldBuildHierarchy() {
        Permission root = build(1L, 0L, "system");
        Permission user = build(2L, 1L, "system:user");
        Permission list = build(3L, 2L, "system:user:list");
        when(permissionRepository.findAllByOrderBySortAscIdAsc())
                .thenReturn(List.of(root, user, list));
        List<PermissionResp> tree = service.tree();
        assertThat(tree).hasSize(1);
        assertThat(tree.get(0).getChildren()).hasSize(1);
        assertThat(tree.get(0).getChildren().get(0).getChildren()).hasSize(1);
        assertThat(tree.get(0).getChildren().get(0).getChildren().get(0).getCode())
                .isEqualTo("system:user:list");
    }

    @Test
    void listCodesByIdsShouldSkipDisabled() {
        Permission enabled = build(1L, 0L, "code1");
        enabled.setStatus(1);
        Permission disabled = build(2L, 0L, "code2");
        disabled.setStatus(0);
        when(permissionRepository.findByIdIn(List.of(1L, 2L))).thenReturn(List.of(enabled, disabled));
        Set<String> codes = service.listCodesByIds(List.of(1L, 2L));
        assertThat(codes).containsExactly("code1");
        // 不调用 evict
        verify(authCacheEvictor, never()).evictAll();
        // 不会触发任何写操作
        verify(permissionRepository, never()).save(any());
    }

    private static Permission build(long id, long parentId, String code) {
        Permission p = new Permission();
        p.setId(id);
        p.setParentId(parentId);
        p.setCode(code);
        p.setName(code);
        p.setType(1);
        p.setSort(10);
        p.setStatus(1);
        p.setIsSystem(false);
        return p;
    }
}
