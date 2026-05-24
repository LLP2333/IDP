package com.qvqw.idp.role.internal;

import com.qvqw.idp.role.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 角色实体的 JPA Repository。
 */
public interface RoleRepository extends JpaRepository<Role, Long> {

    /**
     * 按编码精确查找。
     *
     * @param code 角色编码
     * @return 角色；不存在时 {@code Optional.empty()}
     */
    Optional<Role> findByCode(String code);

    /**
     * 编码是否存在（新增 / 改 code 时唯一性校验）。
     *
     * @param code 角色编码
     * @return 存在返回 {@code true}
     */
    boolean existsByCode(String code);

    /**
     * 同时按 name 和 code 模糊匹配的分页查询。
     *
     * @param name     name 关键字（忽略大小写）
     * @param code     code 关键字（忽略大小写）
     * @param pageable 分页参数
     * @return 角色分页
     */
    Page<Role> findByNameContainingIgnoreCaseOrCodeContainingIgnoreCase(String name, String code, Pageable pageable);

    /**
     * 按 sort 升序列出所有角色。
     *
     * @return 角色列表
     */
    List<Role> findAllByOrderBySortAsc();
}
