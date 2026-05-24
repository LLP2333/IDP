package com.qvqw.idp.dict.internal;

import com.qvqw.idp.dict.Dict;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 字典实体的 JPA Repository。
 */
public interface DictRepository extends JpaRepository<Dict, Long> {

    /**
     * 按编码精确查找。
     *
     * @param code 字典编码
     * @return 字典；不存在时 {@code Optional.empty()}
     */
    Optional<Dict> findByCode(String code);

    /**
     * 编码是否存在。
     *
     * @param code 字典编码
     * @return 存在则返回 {@code true}
     */
    boolean existsByCode(String code);

    /**
     * 按 ID 升序列出全部字典。
     *
     * @return 字典列表
     */
    List<Dict> findAllByOrderByIdAsc();
}
