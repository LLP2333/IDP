package com.qvqw.idp.user.internal;

import com.qvqw.idp.common.cache.AuthCacheEvictor;
import com.qvqw.idp.common.api.PageResp;
import com.qvqw.idp.common.exception.BusinessException;
import com.qvqw.idp.option.OptionService;
import com.qvqw.idp.option.PasswordPolicy;
import com.qvqw.idp.role.RoleService;
import com.qvqw.idp.role.model.resp.RoleResp;
import com.qvqw.idp.user.User;
import com.qvqw.idp.user.UserPasswordHistory;
import com.qvqw.idp.user.UserService;
import com.qvqw.idp.user.model.query.UserQuery;
import com.qvqw.idp.user.model.req.UserBasicInfoUpdateReq;
import com.qvqw.idp.user.model.req.UserCreateReq;
import com.qvqw.idp.user.model.req.UserPasswordChangeReq;
import com.qvqw.idp.user.model.req.UserPasswordResetReq;
import com.qvqw.idp.user.model.req.UserRoleUpdateReq;
import com.qvqw.idp.user.model.req.UserUpdateReq;
import com.qvqw.idp.user.model.resp.UserDetailResp;
import com.qvqw.idp.user.model.resp.UserResp;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
    private final UserPasswordHistoryRepository passwordHistoryRepository;
    private final PasswordValidator passwordValidator;
    private final OptionService optionService;
    private final AuthCacheEvictor authCacheEvictor;

    public UserServiceImpl(UserRepository userRepository,
                           RoleService roleService,
                           PasswordEncoder passwordEncoder,
                           UserPasswordHistoryRepository passwordHistoryRepository,
                           PasswordValidator passwordValidator,
                           OptionService optionService,
                           AuthCacheEvictor authCacheEvictor) {
        this.userRepository = userRepository;
        this.roleService = roleService;
        this.passwordEncoder = passwordEncoder;
        this.passwordHistoryRepository = passwordHistoryRepository;
        this.passwordValidator = passwordValidator;
        this.optionService = optionService;
        this.authCacheEvictor = authCacheEvictor;
    }

    /**
     * 用户分页查询。
     *
     * <p>条件全部使用 JPA Criteria 动态拼装，{@code null} / 空字符串字段直接跳过，
     * 不会作为参数下推到 SQL，从根上避免 Hibernate 7 + PostgreSQL 在
     * {@code (?  is null or lower(...) ...)} 写法下把 {@code null} 推断为
     * {@code bytea} 而抛出 {@code function lower(bytea) does not exist} 的问题。</p>
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
        String username = query == null || isBlank(query.getUsername()) ? null : query.getUsername().trim();
        Integer status = query == null ? null : query.getStatus();
        Specification<User> spec = buildPageSpec(username, status);
        Page<User> result = userRepository.findAll(spec, pageable);
        return PageResp.from(result, this::toResp);
    }

    /**
     * 构造分页查询的动态条件。null/blank 字段不会出现在最终 SQL 中。
     */
    private static Specification<User> buildPageSpec(String username, Integer status) {
        return (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>(2);
            if (username != null && !username.isBlank()) {
                String pattern = "%" + username.toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("username")), pattern));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };
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
        passwordValidator.validateStrength(req.getPassword(), req.getUsername());
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
        passwordHistoryRepository.save(new UserPasswordHistory(id, user.getPassword()));
        if (req.getRoleIds() != null) {
            roleService.assignRoles(id, req.getRoleIds());
            authCacheEvictor.evictUser(id);
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
            authCacheEvictor.evictUser(id);
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
            passwordHistoryRepository.deleteByUserId(id);
            authCacheEvictor.evictUser(id);
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
        passwordValidator.validateStrength(req.getNewPassword(), user.getUsername());
        applyNewPassword(user, req.getNewPassword());
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
        authCacheEvictor.evictUser(id);
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
                .map(u -> new UserCredential(u.getId(), u.getUsername(), u.getPassword(), u.getStatus(),
                        u.getPwdResetAt(), u.getPwdErrorCount(), u.getPwdLockedUntil()));
    }

    /**
     * 按 ID 查找用户详情（供 auth 模块装填 UserContext）。
     */
    @Override
    public Optional<UserDetailResp> findById(Long id) {
        return userRepository.findById(id).map(this::toDetail);
    }

    /**
     * 当前用户自助修改基本信息：仅允许更新昵称 / 邮箱 / 手机 / 性别。
     *
     * <p>对 {@code email} / {@code phone}：</p>
     * <ul>
     *   <li>{@code null} 表示不修改；</li>
     *   <li>{@code ""} 表示清空（数据库置为 {@code null}）；</li>
     *   <li>非空字符串走 DTO 上 {@code @Email} / {@code @Pattern} 校验后落库。</li>
     * </ul>
     *
     * @param userId 当前用户 ID
     * @param req    待更新字段
     * @throws BusinessException 用户不存在
     */
    @Override
    @Transactional
    public void updateCurrentUserBasicInfo(Long userId, UserBasicInfoUpdateReq req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        if (req.getNickname() != null) {
            String nickname = req.getNickname().trim();
            if (nickname.isEmpty()) {
                throw new BusinessException("昵称不能为空");
            }
            user.setNickname(nickname);
        }
        if (req.getEmail() != null) {
            user.setEmail(req.getEmail().isBlank() ? null : req.getEmail().trim());
        }
        if (req.getPhone() != null) {
            user.setPhone(req.getPhone().isBlank() ? null : req.getPhone().trim());
        }
        if (req.getGender() != null) {
            int g = req.getGender();
            if (g != 0 && g != 1 && g != 2) {
                throw new BusinessException("性别取值非法");
            }
            user.setGender(g);
        }
        userRepository.save(user);
    }

    /**
     * 当前用户自助修改密码：校验旧密码 → 校验新密码强度 → 校验历史重复 → 落库。
     *
     * @param userId 当前用户 ID
     * @param req    旧 / 新密码
     * @throws BusinessException 旧密码错误 / 强度不足 / 命中历史
     */
    @Override
    @Transactional
    public void changeCurrentPassword(Long userId, UserPasswordChangeReq req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        if (!passwordEncoder.matches(req.getOldPassword(), user.getPassword())) {
            throw new BusinessException("原密码错误");
        }
        if (req.getOldPassword().equals(req.getNewPassword())) {
            throw new BusinessException("新密码不能与原密码相同");
        }
        passwordValidator.validate(req.getNewPassword(), user.getUsername(),
                passwordHistoryRepository.findRecentByUserId(userId));
        applyNewPassword(user, req.getNewPassword());
        userRepository.save(user);
    }

    @Override
    @Transactional
    public int increasePwdErrorCount(Long userId, int maxCount, LocalDateTime lockUntil) {
        Optional<User> opt = userRepository.findById(userId);
        if (opt.isEmpty()) {
            return 0;
        }
        User user = opt.get();
        int current = (user.getPwdErrorCount() == null ? 0 : user.getPwdErrorCount()) + 1;
        user.setPwdErrorCount(current);
        if (maxCount > 0 && current >= maxCount && lockUntil != null) {
            user.setPwdLockedUntil(lockUntil);
        }
        userRepository.save(user);
        return current;
    }

    @Override
    @Transactional
    public void resetPwdErrorAndUnlock(Long userId) {
        Optional<User> opt = userRepository.findById(userId);
        if (opt.isEmpty()) {
            return;
        }
        User user = opt.get();
        if ((user.getPwdErrorCount() != null && user.getPwdErrorCount() > 0)
                || user.getPwdLockedUntil() != null) {
            user.setPwdErrorCount(0);
            user.setPwdLockedUntil(null);
            userRepository.save(user);
        }
    }

    /**
     * 写入新密码、刷新 pwdResetAt、解锁、写入历史并裁剪保留前 N 条。
     *
     * @param user     目标用户
     * @param newPlain 新密码明文
     */
    private void applyNewPassword(User user, String newPlain) {
        String hash = passwordEncoder.encode(newPlain);
        user.setPassword(hash);
        user.setPwdResetAt(LocalDateTime.now());
        user.setPwdErrorCount(0);
        user.setPwdLockedUntil(null);
        passwordHistoryRepository.save(new UserPasswordHistory(user.getId(), hash));
        int keep = optionService.getIntOrDefault(PasswordPolicy.PASSWORD_REPETITION_TIMES.name(), 3);
        if (keep > 0) {
            List<UserPasswordHistory> all = passwordHistoryRepository.findRecentByUserId(user.getId());
            if (all.size() > keep) {
                List<UserPasswordHistory> toDelete = all.subList(keep, all.size());
                passwordHistoryRepository.deleteAll(toDelete);
            }
        }
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
