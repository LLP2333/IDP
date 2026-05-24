package com.qvqw.idp.role.internal;

import com.qvqw.idp.common.api.PageResp;
import com.qvqw.idp.common.cache.AuthCacheEvictor;
import com.qvqw.idp.common.exception.BusinessException;
import com.qvqw.idp.permission.PermissionService;
import com.qvqw.idp.role.Role;
import com.qvqw.idp.role.RolePermission;
import com.qvqw.idp.role.RoleService;
import com.qvqw.idp.role.UserRole;
import com.qvqw.idp.role.model.query.RoleQuery;
import com.qvqw.idp.role.model.req.RoleReq;
import com.qvqw.idp.role.model.resp.RoleResp;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 角色服务实现：维护 {@code idp_sys_role} 与 {@code idp_sys_user_role} 两张表。
 */
@Service
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PermissionService permissionService;
    private final AuthCacheEvictor authCacheEvictor;

    public RoleServiceImpl(RoleRepository roleRepository,
                           UserRoleRepository userRoleRepository,
                           RolePermissionRepository rolePermissionRepository,
                           PermissionService permissionService,
                           AuthCacheEvictor authCacheEvictor) {
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.permissionService = permissionService;
        this.authCacheEvictor = authCacheEvictor;
    }

    /**
     * 角色分页查询。
     *
     * <p>通过 JPA Criteria 动态拼装条件，null/空字段不会下推到 SQL，规避
     * Hibernate 7 + PostgreSQL 在 null 参数推断为 {@code bytea} 时
     * {@code lower(bytea) does not exist} 的报错。</p>
     *
     * @param query 关键字 / 状态条件（可为 {@code null}）
     * @param page  页码（从 1 开始）
     * @param size  每页大小
     * @return 分页结果（按 sort 升序）
     */
    @Override
    public PageResp<RoleResp> page(RoleQuery query, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), size, Sort.by(Sort.Direction.ASC, "sort"));
        String keyword = query == null ? null : query.getKeyword();
        Integer status = query == null ? null : query.getStatus();
        Specification<Role> spec = buildPageSpec(keyword, status);
        Page<Role> result = roleRepository.findAll(spec, pageable);
        return PageResp.from(result, this::toResp);
    }

    /**
     * 构造角色分页的动态条件：keyword 同时匹配 name / code（忽略大小写），status 等值过滤。
     */
    private static Specification<Role> buildPageSpec(String keyword, Integer status) {
        return (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>(2);
            if (StringUtils.hasText(keyword)) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                Predicate nameLike = cb.like(cb.lower(root.get("name")), pattern);
                Predicate codeLike = cb.like(cb.lower(root.get("code")), pattern);
                predicates.add(cb.or(nameLike, codeLike));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * 不分页查询所有角色，可选按状态过滤；用于下拉选择等场景。
     *
     * @param query 状态过滤（可为 {@code null}）
     * @return 角色列表（按 sort 升序）
     */
    @Override
    public List<RoleResp> list(RoleQuery query) {
        return roleRepository.findAllByOrderBySortAsc().stream()
                .filter(r -> query == null || query.getStatus() == null || query.getStatus().equals(r.getStatus()))
                .map(this::toResp)
                .toList();
    }

    /**
     * 查询角色详情。
     *
     * @param id 角色 ID
     * @return 角色 DTO
     * @throws BusinessException 角色不存在
     */
    @Override
    public RoleResp get(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new BusinessException("角色不存在"));
        return toResp(role);
    }

    /**
     * 按编码查找角色，供其他模块（如 user 模块装配角色）使用。
     *
     * @param code 角色编码
     * @return 角色；不存在时 {@code Optional.empty()}
     */
    @Override
    public java.util.Optional<RoleResp> findByCode(String code) {
        return roleRepository.findByCode(code).map(this::toResp);
    }

    /**
     * 新增角色：code 唯一校验通过后落库，默认非系统角色。
     *
     * @param req 新增请求
     * @return 角色 ID
     * @throws BusinessException 编码已存在
     */
    @Override
    @Transactional
    public Long create(RoleReq req) {
        if (roleRepository.existsByCode(req.getCode())) {
            throw new BusinessException("角色编码已存在");
        }
        Role role = new Role();
        role.setName(req.getName());
        role.setCode(req.getCode());
        role.setDescription(req.getDescription());
        role.setSort(req.getSort() == null ? 999 : req.getSort());
        role.setStatus(req.getStatus() == null ? 1 : req.getStatus());
        role.setIsSystem(false);
        return roleRepository.save(role).getId();
    }

    /**
     * 修改角色：
     * <ul>
     *   <li>系统内置角色禁止改 code；</li>
     *   <li>code 变更时校验唯一。</li>
     * </ul>
     *
     * @param id  角色 ID
     * @param req 修改请求
     * @throws BusinessException 角色不存在 / 编码冲突 / 系统内置角色改 code
     */
    @Override
    @Transactional
    public void update(Long id, RoleReq req) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new BusinessException("角色不存在"));
        if (Boolean.TRUE.equals(role.getIsSystem()) && !role.getCode().equals(req.getCode())) {
            throw new BusinessException("系统内置角色不允许修改编码");
        }
        if (!role.getCode().equals(req.getCode()) && roleRepository.existsByCode(req.getCode())) {
            throw new BusinessException("角色编码已存在");
        }
        role.setName(req.getName());
        role.setCode(req.getCode());
        role.setDescription(req.getDescription());
        if (req.getSort() != null) {
            role.setSort(req.getSort());
        }
        if (req.getStatus() != null) {
            role.setStatus(req.getStatus());
        }
        roleRepository.save(role);
    }

    /**
     * 批量删除角色。
     *
     * <p>预校验全部通过后再删除；任一角色为系统内置或仍被用户引用时整体失败回滚。</p>
     *
     * @param ids 待删除的角色 ID 列表
     * @throws BusinessException 角色不存在 / 系统内置 / 仍有用户绑定
     */
    @Override
    @Transactional
    public void delete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (Long id : ids) {
            Role role = roleRepository.findById(id)
                    .orElseThrow(() -> new BusinessException("角色不存在: " + id));
            if (Boolean.TRUE.equals(role.getIsSystem())) {
                throw new BusinessException("系统内置角色不允许删除: " + role.getName());
            }
            if (userRoleRepository.countByRoleId(id) > 0) {
                throw new BusinessException("角色已分配给用户，请先解除分配: " + role.getName());
            }
        }
        // 先解除角色 - 权限关联，再删除角色本身
        for (Long id : ids) {
            rolePermissionRepository.deleteByRoleId(id);
        }
        roleRepository.deleteAllById(ids);
        authCacheEvictor.evictAll();
    }

    /**
     * 查询用户下所有角色编码（用于 JWT 过滤器装填权限）。
     *
     * @param userId 用户 ID（可为 {@code null}）
     * @return 角色编码集合；用户不存在或无角色时为空集
     */
    @Override
    public Set<String> listCodesByUserId(Long userId) {
        if (userId == null) {
            return new HashSet<>();
        }
        List<Long> roleIds = userRoleRepository.findRoleIdsByUserId(userId);
        if (roleIds.isEmpty()) {
            return new HashSet<>();
        }
        return roleRepository.findAllById(roleIds).stream()
                .map(Role::getCode)
                .collect(Collectors.toCollection(HashSet::new));
    }

    /**
     * 列出某角色下的所有用户 ID。
     *
     * @param roleId 角色 ID
     * @return 用户 ID 列表
     */
    @Override
    public List<Long> listUserIdsByRoleId(Long roleId) {
        return userRoleRepository.findUserIdsByRoleId(roleId);
    }

    /**
     * 列出某用户拥有的所有角色 ID。
     *
     * @param userId 用户 ID
     * @return 角色 ID 列表
     */
    @Override
    public List<Long> listRoleIdsByUserId(Long userId) {
        return userRoleRepository.findRoleIdsByUserId(userId);
    }

    /**
     * 重新分配某用户的角色集合（全量覆盖）。
     *
     * <p>实现：先清空原有记录，再批量写入新记录；传入空列表表示清空角色。</p>
     *
     * @param userId  用户 ID
     * @param roleIds 角色 ID 列表
     * @throws BusinessException 角色 ID 非法
     */
    @Override
    @Transactional
    public void assignRoles(Long userId, List<Long> roleIds) {
        userRoleRepository.deleteByUserId(userId);
        if (roleIds == null || roleIds.isEmpty()) {
            authCacheEvictor.evictUser(userId);
            return;
        }
        ensureRolesExist(roleIds);
        List<UserRole> entities = roleIds.stream().distinct()
                .map(rid -> new UserRole(userId, rid))
                .toList();
        userRoleRepository.saveAll(entities);
        authCacheEvictor.evictUser(userId);
    }

    /**
     * 校验所有角色 ID 是否合法存在；只要有一个 ID 在 {@code idp_sys_role} 中找不到则抛业务异常。
     *
     * @param roleIds 待校验的角色 ID 列表
     * @throws BusinessException 存在非法角色 ID
     */
    @Override
    public void ensureRolesExist(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        long count = roleRepository.findAllById(roleIds).size();
        if (count != roleIds.stream().distinct().count()) {
            throw new BusinessException("存在无效的角色 ID");
        }
    }

    /**
     * 查询某角色已绑定的全部权限 ID。
     *
     * @param roleId 角色 ID
     * @return 权限 ID 列表（不可为 {@code null}）
     */
    @Override
    public List<Long> listPermissionIdsByRoleId(Long roleId) {
        if (roleId == null) {
            return new ArrayList<>();
        }
        return rolePermissionRepository.findPermissionIdsByRoleId(roleId);
    }

    /**
     * 重新分配某角色的权限集合（全量覆盖）。
     *
     * <p>admin 角色由 {@code RolePermissionSeeder} 维护，禁止通过该接口修改；如果传入 admin
     * 会直接抛业务异常。</p>
     *
     * @param roleId        角色 ID
     * @param permissionIds 权限 ID 列表（可为空，代表清空权限）
     * @throws BusinessException 角色不存在 / 是 admin / 权限 ID 非法
     */
    @Override
    @Transactional
    public void assignPermissions(Long roleId, List<Long> permissionIds) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new BusinessException("角色不存在"));
        if ("admin".equals(role.getCode())) {
            throw new BusinessException("admin 角色拥有全部权限，无需也不允许通过该接口分配");
        }
        rolePermissionRepository.deleteByRoleId(roleId);
        if (permissionIds == null || permissionIds.isEmpty()) {
            return;
        }
        List<Long> distinct = permissionIds.stream().distinct().toList();
        Set<String> codes = permissionService.listCodesByIds(distinct);
        if (codes.size() != distinct.size()) {
            throw new BusinessException("存在无效的权限 ID 或权限已禁用");
        }
        List<RolePermission> entities = distinct.stream()
                .map(pid -> new RolePermission(roleId, pid))
                .toList();
        rolePermissionRepository.saveAll(entities);
        // 角色权限变化后，该角色下所有用户的权限缓存都需要失效
        List<Long> userIds = userRoleRepository.findUserIdsByRoleId(roleId);
        authCacheEvictor.evictUsers(userIds);
    }

    /**
     * 聚合用户 → 角色 → 权限码集合。
     *
     * @param userId 用户 ID
     * @return 权限码集合
     */
    @Override
    public Set<String> listPermissionCodesByUserId(Long userId) {
        if (userId == null) {
            return new HashSet<>();
        }
        List<Long> roleIds = userRoleRepository.findRoleIdsByUserId(userId);
        if (roleIds.isEmpty()) {
            return new HashSet<>();
        }
        List<Long> permissionIds = rolePermissionRepository.findPermissionIdsByRoleIds(roleIds);
        return permissionService.listCodesByIds(permissionIds);
    }

    /**
     * 实体 → DTO 的简单映射。
     */
    private RoleResp toResp(Role role) {
        RoleResp resp = new RoleResp();
        resp.setId(role.getId());
        resp.setName(role.getName());
        resp.setCode(role.getCode());
        resp.setDescription(role.getDescription());
        resp.setSort(role.getSort());
        resp.setStatus(role.getStatus());
        resp.setIsSystem(role.getIsSystem());
        resp.setCreatedAt(role.getCreatedAt());
        resp.setUpdatedAt(role.getUpdatedAt());
        return resp;
    }
}
