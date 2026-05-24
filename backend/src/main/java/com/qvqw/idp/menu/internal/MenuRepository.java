package com.qvqw.idp.menu.internal;

import com.qvqw.idp.menu.Menu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * {@link Menu} 的 JPA Repository。
 */
public interface MenuRepository extends JpaRepository<Menu, Long>, JpaSpecificationExecutor<Menu> {

    Optional<Menu> findByPermission(String permission);

    boolean existsByPermission(String permission);

    /** 用于校验同级标题不重复。 */
    boolean existsByParentIdAndTitle(Long parentId, String title);

    List<Menu> findAllByOrderBySortAscIdAsc();

    List<Menu> findByIdIn(Collection<Long> ids);

    List<Menu> findByIsSystemTrueOrderBySortAscIdAsc();

    long countByParentId(Long parentId);
}
