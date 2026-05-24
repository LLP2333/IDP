package com.qvqw.idp.user.internal;

import com.qvqw.idp.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 用户实体的 JPA Repository。
 */
public interface UserRepository extends JpaRepository<User, Long> {

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
     * 用户名 + 状态的可选过滤分页查询。
     *
     * @param username 用户名关键字（{@code null} 时不过滤）
     * @param status   状态（{@code null} 时不过滤）
     * @param pageable 分页参数
     * @return 用户分页
     */
    @Query("select u from User u where (:username is null or lower(u.username) like lower(concat('%', :username, '%'))) " +
            "and (:status is null or u.status = :status)")
    Page<User> search(@Param("username") String username, @Param("status") Integer status, Pageable pageable);

    /**
     * 按 ID 列表批量获取用户。
     *
     * @param ids 用户 ID 列表
     * @return 用户列表
     */
    List<User> findAllByIdIn(List<Long> ids);
}
