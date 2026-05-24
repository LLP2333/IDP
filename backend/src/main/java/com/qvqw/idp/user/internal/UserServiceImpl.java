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

    @Override
    public PageResp<UserResp> page(UserQuery query, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), size,
                Sort.by(Sort.Direction.DESC, "createdAt"));
        String username = query == null || isBlank(query.getUsername()) ? null : query.getUsername();
        Integer status = query == null ? null : query.getStatus();
        Page<User> result = userRepository.search(username, status, pageable);
        return PageResp.from(result, this::toResp);
    }

    @Override
    public UserDetailResp get(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        return toDetail(user);
    }

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

    @Override
    @Transactional
    public void resetPassword(Long id, UserPasswordResetReq req) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        user.setPwdResetAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void updateRole(Long id, UserRoleUpdateReq req) {
        if (!userRepository.existsById(id)) {
            throw new BusinessException("用户不存在");
        }
        roleService.assignRoles(id, req.getRoleIds());
    }

    @Override
    public Optional<UserCredential> findCredential(String username) {
        return userRepository.findByUsername(username)
                .map(u -> new UserCredential(u.getId(), u.getUsername(), u.getPassword(), u.getStatus()));
    }

    @Override
    public Optional<UserDetailResp> findById(Long id) {
        return userRepository.findById(id).map(this::toDetail);
    }

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

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
