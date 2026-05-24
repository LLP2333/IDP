package com.qvqw.idp.role.internal;

import com.qvqw.idp.role.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

/**
 * 角色实体的 JPA Repository。
 *
 * <p>继承 {@link JpaSpecificationExecutor} 以支持 Service 层用 Criteria API 拼装
 * 多条件可空的分页查询，规避 Hibernate 7 + PostgreSQL 在 {@code (:param is null or ...)}
 * 写法下 null 参数被识别为 {@code bytea} 的兼容性问题。</p>
 */
public interface RoleRepository extends JpaRepository<Role, Long>, JpaSpecificationExecutor<Role> {

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
     * 按 sort 升序列出所有角色。
     *
     * @return 角色列表
     */
    List<Role> findAllByOrderBySortAsc();
}
