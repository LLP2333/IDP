package com.qvqw.idp.role.internal;

import com.qvqw.idp.common.api.PageResp;
import com.qvqw.idp.common.exception.BusinessException;
import com.qvqw.idp.role.Role;
import com.qvqw.idp.role.RoleService;
import com.qvqw.idp.role.UserRole;
import com.qvqw.idp.role.model.query.RoleQuery;
import com.qvqw.idp.role.model.req.RoleReq;
import com.qvqw.idp.role.model.resp.RoleResp;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;

    public RoleServiceImpl(RoleRepository roleRepository, UserRoleRepository userRoleRepository) {
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
    }

    @Override
    public PageResp<RoleResp> page(RoleQuery query, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), size, Sort.by(Sort.Direction.ASC, "sort"));
        String keyword = query == null ? null : query.getKeyword();
        Page<Role> result;
        if (StringUtils.hasText(keyword)) {
            result = roleRepository.findByNameContainingIgnoreCaseOrCodeContainingIgnoreCase(keyword, keyword, pageable);
        } else {
            result = roleRepository.findAll(pageable);
        }
        return PageResp.from(result, this::toResp);
    }

    @Override
    public List<RoleResp> list(RoleQuery query) {
        return roleRepository.findAllByOrderBySortAsc().stream()
                .filter(r -> query == null || query.getStatus() == null || query.getStatus().equals(r.getStatus()))
                .map(this::toResp)
                .toList();
    }

    @Override
    public RoleResp get(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new BusinessException("角色不存在"));
        return toResp(role);
    }

    @Override
    public java.util.Optional<RoleResp> findByCode(String code) {
        return roleRepository.findByCode(code).map(this::toResp);
    }

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
        roleRepository.deleteAllById(ids);
    }

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

    @Override
    public List<Long> listUserIdsByRoleId(Long roleId) {
        return userRoleRepository.findUserIdsByRoleId(roleId);
    }

    @Override
    public List<Long> listRoleIdsByUserId(Long userId) {
        return userRoleRepository.findRoleIdsByUserId(userId);
    }

    @Override
    @Transactional
    public void assignRoles(Long userId, List<Long> roleIds) {
        userRoleRepository.deleteByUserId(userId);
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        ensureRolesExist(roleIds);
        List<UserRole> entities = roleIds.stream().distinct()
                .map(rid -> new UserRole(userId, rid))
                .toList();
        userRoleRepository.saveAll(entities);
    }

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
