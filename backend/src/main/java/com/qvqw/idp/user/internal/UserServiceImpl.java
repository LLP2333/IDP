package com.qvqw.idp.user.internal;

import com.qvqw.idp.common.api.PageResp;
import com.qvqw.idp.common.exception.BusinessException;
import com.qvqw.idp.role.RoleService;
import com.qvqw.idp.role.model.resp.RoleResp;
import com.qvqw.idp.user.User;
import com.qvqw.idp.user.UserService;
import com.qvqw.idp.user.model.query.UserQuery;
import com.qvqw.idp.user.model.req.UserCreateReq;
import com.qvqw.idp.user.model.req.UserPasswordResetReq;
import com.qvqw.idp.user.model.req.UserRoleUpdateReq;
import com.qvqw.idp.user.model.req.UserUpdateReq;
import com.qvqw.idp.user.model.resp.UserDetailResp;
import com.qvqw.idp.user.model.resp.UserResp;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 用户服务实现。
 *
 * <p>除查询类方法外，写入类方法均使用 {@link Transactional} 包裹，以保证 “用户主表” 与
 * “用户-角色关联表” 的修改原子提交。</p>
 */
@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleService roleService;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository,
                           RoleService roleService,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleService = roleService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 用户分页查询。
     *
     * @param query 查询条件（用户名、状态），可为 {@code null}
     * @param page  页码（从 1 开始）
     * @param size  每页大小
     * @return 分页列表
     */
    @Override
    public PageResp<UserResp> page(UserQuery query, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), size,
                Sort.by(Sort.Direction.DESC, "createdAt"));
        String username = query == null || isBlank(query.getUsername()) ? null : query.getUsername();
        Integer status = query == null ? null : query.getStatus();
        Page<User> result = userRepository.search(username, status, pageable);
        return PageResp.from(result, this::toResp);
    }

    /**
     * 查询用户详情（含角色 ID / code / name 列表）。
     *
     * @param id 用户 ID
     * @return 用户详情
     * @throws BusinessException 用户不存在
     */
    @Override
    public UserDetailResp get(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        return toDetail(user);
    }

    /**
     * 新增用户：
     * <ol>
     *   <li>校验用户名唯一；</li>
     *   <li>若指定 roleIds，先校验是否全部存在；</li>
     *   <li>BCrypt 加密密码后保存；</li>
     *   <li>调用 RoleService 完成角色关联。</li>
     * </ol>
     *
     * @param req 新增请求
     * @return 新建用户 ID
     * @throws BusinessException 用户名已存在 / 角色 ID 非法
     */
    @Override
    @Transactional
    public Long create(UserCreateReq req) {
        if (userRepository.existsByUsername(req.getUsername())) {
            throw new BusinessException("用户名已存在");
        }
        if (req.getRoleIds() != null && !req.getRoleIds().isEmpty()) {
            roleService.ensureRolesExist(req.getRoleIds());
        }
        User user = new User();
        user.setUsername(req.getUsername());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setNickname(isBlank(req.getNickname()) ? req.getUsername() : req.getNickname());
        user.setEmail(req.getEmail());
        user.setPhone(req.getPhone());
        user.setGender(req.getGender() == null ? 0 : req.getGender());
        user.setDescription(req.getDescription());
        user.setStatus(req.getStatus() == null ? 1 : req.getStatus());
        user.setIsSystem(false);
        user.setPwdResetAt(LocalDateTime.now());
        Long id = userRepository.save(user).getId();
        if (req.getRoleIds() != null) {
            roleService.assignRoles(id, req.getRoleIds());
        }
        return id;
    }

    /**
     * 修改用户基本信息；用户名不可改，{@code req} 中 {@code null} 字段不会覆盖。
     *
     * <p>系统内置用户不允许将 status 改为 0（禁用）。</p>
     *
     * @param id  用户 ID
     * @param req 更新请求
     * @throws BusinessException 用户不存在 / 系统内置用户被禁用
     */
    @Override
    @Transactional
    public void update(Long id, UserUpdateReq req) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        if (req.getNickname() != null) {
            user.setNickname(req.getNickname());
        }
        if (req.getEmail() != null) {
            user.setEmail(req.getEmail());
        }
        if (req.getPhone() != null) {
            user.setPhone(req.getPhone());
        }
        if (req.getGender() != null) {
            user.setGender(req.getGender());
        }
        if (req.getDescription() != null) {
            user.setDescription(req.getDescription());
        }
        if (req.getStatus() != null) {
            if (Boolean.TRUE.equals(user.getIsSystem()) && req.getStatus() == 0) {
                throw new BusinessException("系统内置用户不允许禁用");
            }
            user.setStatus(req.getStatus());
        }
        userRepository.save(user);
        if (req.getRoleIds() != null) {
            roleService.assignRoles(id, req.getRoleIds());
        }
    }

    /**
     * 批量删除用户：先解除角色绑定，再删除用户实体。
     *
     * <p>系统内置用户不允许删除，遇到则整体抛业务异常并回滚。</p>
     *
     * @param ids 用户 ID 列表
     * @throws BusinessException 用户不存在 / 系统内置用户
     */
    @Override
    @Transactional
    public void delete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (Long id : ids) {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new BusinessException("用户不存在: " + id));
            if (Boolean.TRUE.equals(user.getIsSystem())) {
                throw new BusinessException("系统内置用户不允许删除: " + user.getUsername());
            }
        }
        for (Long id : ids) {
            roleService.assignRoles(id, List.of());
        }
        userRepository.deleteAllById(ids);
    }

    /**
     * 管理员重置密码：覆盖 hash + 刷新 {@code pwdResetAt}。
     *
     * @param id  用户 ID
     * @param req 新密码
     * @throws BusinessException 用户不存在
     */
    @Override
    @Transactional
    public void resetPassword(Long id, UserPasswordResetReq req) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        user.setPwdResetAt(LocalDateTime.now());
        userRepository.save(user);
    }

    /**
     * 重新分配用户的角色（全量覆盖）。
     *
     * @param id  用户 ID
     * @param req 角色 ID 列表
     * @throws BusinessException 用户不存在 / 角色 ID 非法
     */
    @Override
    @Transactional
    public void updateRole(Long id, UserRoleUpdateReq req) {
        if (!userRepository.existsById(id)) {
            throw new BusinessException("用户不存在");
        }
        roleService.assignRoles(id, req.getRoleIds());
    }

    /**
     * 供 auth 模块在登录时校验账号使用，返回包含原始密码哈希的轻量凭证。
     *
     * @param username 用户名
     * @return 凭证；用户不存在时 {@code Optional.empty()}
     */
    @Override
    public Optional<UserCredential> findCredential(String username) {
        return userRepository.findByUsername(username)
                .map(u -> new UserCredential(u.getId(), u.getUsername(), u.getPassword(), u.getStatus()));
    }

    /**
     * 按 ID 查找用户详情（供 auth 模块装填 UserContext）。
     */
    @Override
    public Optional<UserDetailResp> findById(Long id) {
        return userRepository.findById(id).map(this::toDetail);
    }

    /**
     * 实体 → 列表 DTO 的精简映射。
     */
    private UserResp toResp(User user) {
        UserResp resp = new UserResp();
        resp.setId(user.getId());
        resp.setUsername(user.getUsername());
        resp.setNickname(user.getNickname());
        resp.setEmail(user.getEmail());
        resp.setPhone(user.getPhone());
        resp.setGender(user.getGender());
        resp.setAvatar(user.getAvatar());
        resp.setStatus(user.getStatus());
        resp.setIsSystem(user.getIsSystem());
        resp.setCreatedAt(user.getCreatedAt());
        resp.setUpdatedAt(user.getUpdatedAt());
        return resp;
    }

    /**
     * 实体 → 详情 DTO 的映射，同时填充角色信息。
     *
     * <p>对每个角色 ID 调用 {@link RoleService#get(Long)}；如该 ID 已被删除，忽略不抛出，
     * 让接口仍然能完整返回。</p>
     */
    private UserDetailResp toDetail(User user) {
        UserDetailResp resp = new UserDetailResp();
        resp.setId(user.getId());
        resp.setUsername(user.getUsername());
        resp.setNickname(user.getNickname());
        resp.setEmail(user.getEmail());
        resp.setPhone(user.getPhone());
        resp.setGender(user.getGender());
        resp.setAvatar(user.getAvatar());
        resp.setDescription(user.getDescription());
        resp.setStatus(user.getStatus());
        resp.setIsSystem(user.getIsSystem());
        resp.setCreatedAt(user.getCreatedAt());
        resp.setUpdatedAt(user.getUpdatedAt());
        resp.setPwdResetAt(user.getPwdResetAt());
        List<Long> roleIds = roleService.listRoleIdsByUserId(user.getId());
        resp.setRoleIds(roleIds);
        if (!roleIds.isEmpty()) {
            Map<Long, RoleResp> map = new HashMap<>();
            for (Long rid : roleIds) {
                try {
                    RoleResp r = roleService.get(rid);
                    map.put(rid, r);
                } catch (BusinessException ignore) {
                }
            }
            resp.setRoleCodes(map.values().stream().map(RoleResp::getCode).toList());
            resp.setRoleNames(map.values().stream().map(RoleResp::getName).toList());
        } else {
            resp.setRoleCodes(List.of());
            resp.setRoleNames(List.of());
        }
        return resp;
    }

    /**
     * 空白字符串判断的简化封装，避免引入额外依赖。
     */
    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
