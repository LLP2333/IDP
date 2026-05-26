package com.qvqw.idp.storage.internal;

import com.qvqw.idp.storage.Storage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

/**
 * 存储 Repository。
 *
 * <p>提供基础 CRUD、规范查询、以及按 code / isDefault 的常用查询。</p>
 */
public interface StorageRepository extends JpaRepository<Storage, Long>, JpaSpecificationExecutor<Storage> {

    /** 按 code 查询（唯一索引）。 */
    Optional<Storage> findByCode(String code);

    /** 查询当前默认存储。 */
    Optional<Storage> findFirstByIsDefaultTrue();

    /** 判断 code 是否已存在（排除指定 ID）。 */
    boolean existsByCodeAndIdNot(String code, Long id);

    /** 判断 code 是否已存在。 */
    boolean existsByCode(String code);
}
