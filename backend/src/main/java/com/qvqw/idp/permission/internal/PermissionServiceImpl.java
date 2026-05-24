package com.qvqw.idp.permission.internal;

import com.qvqw.idp.common.cache.AuthCacheEvictor;
import com.qvqw.idp.common.exception.BusinessException;
import com.qvqw.idp.permission.Permission;
import com.qvqw.idp.permission.PermissionService;
import com.qvqw.idp.permission.model.query.PermissionQuery;
import com.qvqw.idp.permission.model.req.PermissionReq;
import com.qvqw.idp.permission.model.resp.PermissionResp;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 权限服务实现。
 *
 * <p>注意：删除权限只校验 {@code is_system} 与是否有子节点；“是否仍被角色引用” 的校验由
 * role 模块在 {@code delete} 链路里独立做（避免与 role 双向依赖）。</p>
 */
@Service
public class PermissionServiceImpl implements PermissionService {

    private final PermissionRepository permissionRepository;
    private final AuthCacheEvictor authCacheEvictor;

    public PermissionServiceImpl(PermissionRepository permissionRepository,
                                 AuthCacheEvictor authCacheEvictor) {
        this.permissionRepository = permissionRepository;
        this.authCacheEvictor = authCacheEvictor;
    }

    @Override
    public List<PermissionResp> list(PermissionQuery query) {
        Specification<Permission> spec = buildSpec(query);
        return permissionRepository.findAll(spec).stream()
                .sorted((a, b) -> {
                    int cmp = Integer.compare(a.getSort(), b.getSort());
                    return cmp != 0 ? cmp : Long.compare(a.getId(), b.getId());
                })
                .map(this::toResp)
                .toList();
    }

    private static Specification<Permission> buildSpec(PermissionQuery query) {
        return (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>(3);
            if (query != null) {
                if (StringUtils.hasText(query.getKeyword())) {
                    String kw = "%" + query.getKeyword().trim().toLowerCase() + "%";
                    predicates.add(cb.or(cb.like(cb.lower(root.get("code")), kw),
                            cb.like(cb.lower(root.get("name")), kw)));
                }
                if (query.getStatus() != null) {
                    predicates.add(cb.equal(root.get("status"), query.getStatus()));
                }
                if (query.getType() != null) {
                    predicates.add(cb.equal(root.get("type"), query.getType()));
                }
            }
            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * 构造树形结构：先按 parentId 分组，再从 parentId=0 递归挂载。
     *
     * @return 顶级权限节点
     */
    @Override
    public List<PermissionResp> tree() {
        List<Permission> all = permissionRepository.findAllByOrderBySortAscIdAsc();
        Map<Long, List<PermissionResp>> byParent = all.stream()
                .map(this::toResp)
                .collect(Collectors.groupingBy(PermissionResp::getParentId));
        List<PermissionResp> roots = byParent.getOrDefault(0L, Collections.emptyList());
        for (PermissionResp root : roots) {
            attachChildren(root, byParent);
        }
        return roots;
    }

    private void attachChildren(PermissionResp node, Map<Long, List<PermissionResp>> byParent) {
        List<PermissionResp> children = byParent.getOrDefault(node.getId(), Collections.emptyList());
        node.setChildren(children);
        for (PermissionResp child : children) {
            attachChildren(child, byParent);
        }
    }

    @Override
    public PermissionResp get(Long id) {
        Permission p = permissionRepository.findById(id)
                .orElseThrow(() -> new BusinessException("权限不存在"));
        return toResp(p);
    }

    /**
     * 新增权限。
     *
     * @param req 请求
     * @return 权限 ID
     * @throws BusinessException 编码已存在 / 父节点不存在
     */
    @Override
    @Transactional
    public Long create(PermissionReq req) {
        if (permissionRepository.existsByCode(req.getCode())) {
            throw new BusinessException("权限编码已存在");
        }
        if (req.getParentId() != null && req.getParentId() > 0
                && !permissionRepository.existsById(req.getParentId())) {
            throw new BusinessException("父权限不存在");
        }
        Permission p = new Permission();
        p.setCode(req.getCode());
        p.setName(req.getName());
        p.setParentId(req.getParentId() == null ? 0L : req.getParentId());
        p.setType(req.getType());
        p.setSort(req.getSort() == null ? 999 : req.getSort());
        p.setStatus(req.getStatus() == null ? 1 : req.getStatus());
        p.setIsSystem(false);
        p.setDescription(req.getDescription());
        return permissionRepository.save(p).getId();
    }

    /**
     * 修改权限：系统内置权限不允许改 code 与父节点。
     */
    /**
     * 修改权限：系统内置权限不允许改 code 与父节点；变更后全量清理鉴权缓存。
     */
    @Override
    @Transactional
    public void update(Long id, PermissionReq req) {
        Permission p = permissionRepository.findById(id)
                .orElseThrow(() -> new BusinessException("权限不存在"));
        if (Boolean.TRUE.equals(p.getIsSystem())) {
            if (!p.getCode().equals(req.getCode())) {
                throw new BusinessException("系统内置权限不允许修改编码");
            }
            if (!p.getParentId().equals(req.getParentId() == null ? 0L : req.getParentId())) {
                throw new BusinessException("系统内置权限不允许修改父节点");
            }
        } else {
            if (!p.getCode().equals(req.getCode()) && permissionRepository.existsByCode(req.getCode())) {
                throw new BusinessException("权限编码已存在");
            }
            p.setCode(req.getCode());
            p.setParentId(req.getParentId() == null ? 0L : req.getParentId());
        }
        p.setName(req.getName());
        p.setType(req.getType());
        if (req.getSort() != null) {
            p.setSort(req.getSort());
        }
        if (req.getStatus() != null) {
            p.setStatus(req.getStatus());
        }
        p.setDescription(req.getDescription());
        permissionRepository.save(p);
        authCacheEvictor.evictAll();
    }

    /**
     * 批量删除权限。
     *
     * <p>校验：系统内置不允许删除；存在子节点不允许删除。<br>
     * “仍被角色引用” 的校验由 role 模块的 {@code RolePermissionRepository#countByPermissionId} 兜底
     * （它会在自身的 listener 中阻止，避免循环依赖；如未实现则交给数据库外键级联）。</p>
     */
    @Override
    @Transactional
    public void delete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (Long id : ids) {
            Permission p = permissionRepository.findById(id)
                    .orElseThrow(() -> new BusinessException("权限不存在: " + id));
            if (Boolean.TRUE.equals(p.getIsSystem())) {
                throw new BusinessException("系统内置权限不允许删除: " + p.getCode());
            }
            if (permissionRepository.countByParentId(id) > 0) {
                throw new BusinessException("存在子节点，请先删除子节点: " + p.getCode());
            }
        }
        permissionRepository.deleteAllById(ids);
        authCacheEvictor.evictAll();
    }

    @Override
    public Set<String> listCodesByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new HashSet<>();
        }
        return permissionRepository.findByIdIn(ids).stream()
                .filter(p -> p.getStatus() != null && p.getStatus() == 1)
                .map(Permission::getCode)
                .collect(Collectors.toCollection(HashSet::new));
    }

    @Override
    public List<Long> listSystemPermissionIds() {
        return permissionRepository.findByIsSystemTrueOrderBySortAscIdAsc().stream()
                .map(Permission::getId)
                .toList();
    }

    private PermissionResp toResp(Permission p) {
        PermissionResp resp = new PermissionResp();
        resp.setId(p.getId());
        resp.setCode(p.getCode());
        resp.setName(p.getName());
        resp.setParentId(p.getParentId());
        resp.setType(p.getType());
        resp.setSort(p.getSort());
        resp.setStatus(p.getStatus());
        resp.setIsSystem(p.getIsSystem());
        resp.setDescription(p.getDescription());
        resp.setCreatedAt(p.getCreatedAt());
        resp.setUpdatedAt(p.getUpdatedAt());
        return resp;
    }
}
