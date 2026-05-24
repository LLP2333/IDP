package com.qvqw.idp.permission.internal;

import com.qvqw.idp.permission.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * {@link Permission} 的 JPA Repository。
 */
public interface PermissionRepository extends JpaRepository<Permission, Long>, JpaSpecificationExecutor<Permission> {

    Optional<Permission> findByCode(String code);

    boolean existsByCode(String code);

    List<Permission> findAllByOrderBySortAscIdAsc();

    List<Permission> findByIdIn(Collection<Long> ids);

    List<Permission> findByIsSystemTrueOrderBySortAscIdAsc();

    long countByParentId(Long parentId);
}
