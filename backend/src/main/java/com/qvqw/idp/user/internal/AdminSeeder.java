package com.qvqw.idp.user.internal;

import com.qvqw.idp.role.RoleService;
import com.qvqw.idp.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 启动时初始化默认超级管理员账号。
 *
 * <p>晚于 {@code RoleSeeder} 执行，依赖 admin 角色已存在。</p>
 */
@Component
@Order(20)
public class AdminSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private static final String DEFAULT_USERNAME = "admin";
    private static final String DEFAULT_PASSWORD = "123456";

    private final UserRepository userRepository;
    private final RoleService roleService;
    private final PasswordEncoder passwordEncoder;

    public AdminSeeder(UserRepository userRepository,
                       RoleService roleService,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleService = roleService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.findByUsername(DEFAULT_USERNAME).isPresent()) {
            return;
        }
        User user = new User();
        user.setUsername(DEFAULT_USERNAME);
        user.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
        user.setNickname("超级管理员");
        user.setStatus(1);
        user.setIsSystem(true);
        user.setGender(0);
        user.setPwdResetAt(LocalDateTime.now());
        User saved = userRepository.save(user);
        roleService.findByCode("admin").ifPresent(role ->
                roleService.assignRoles(saved.getId(), List.of(role.getId())));
        log.info("[初始化] 已创建默认管理员账号: {} / {}", DEFAULT_USERNAME, DEFAULT_PASSWORD);
    }
}
