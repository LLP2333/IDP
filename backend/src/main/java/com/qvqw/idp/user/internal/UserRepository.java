package com.qvqw.idp.user.internal;

import com.qvqw.idp.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

/**
 * 用户实体的 JPA Repository。
 *
 * <p>同时继承 {@link JpaSpecificationExecutor}，便于 Service 层在 “用户名 / 状态” 这类
 * 可空多条件场景下，使用 Criteria API 动态拼装 {@code where} 子句。这样可以避免在 JPQL
 * 中通过 {@code (:param is null or ...)} 的写法把 {@code null} 参数下推到数据库，
 * 进而规避 Hibernate 7 + PostgreSQL 在某些 {@code null} 参数推断为 {@code bytea} 导致
 * {@code lower(bytea) does not exist} 的错误。</p>
 */
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    /**
     * 按用户名精确查找（用户名全局唯一）。
     *
     * @param username 用户名
     * @return 用户；不存在时 {@code Optional.empty()}
     */
    Optional<User> findByUsername(String username);

    /**
     * 用户名是否存在（新增用户时唯一性校验）。
     *
     * @param username 用户名
     * @return 存在返回 {@code true}
     */
    boolean existsByUsername(String username);

    /**
     * 按 ID 列表批量获取用户。
     *
     * @param ids 用户 ID 列表
     * @return 用户列表
     */
    List<User> findAllByIdIn(List<Long> ids);
}
