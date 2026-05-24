package com.qvqw.idp.dict.internal;

import com.qvqw.idp.dict.DictItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * 字典明细的 JPA Repository。
 */
public interface DictItemRepository extends JpaRepository<DictItem, Long> {

    /**
     * 列出某字典下的全部明细（按 sort 升序）。
     *
     * @param dictId 字典 ID
     * @return 明细列表
     */
    List<DictItem> findAllByDictIdOrderBySortAsc(Long dictId);

    /**
     * 列出某字典下的启用明细（按 sort 升序）。
     *
     * @param dictId 字典 ID
     * @return 启用明细列表
     */
    List<DictItem> findAllByDictIdAndStatusOrderBySortAsc(Long dictId, Integer status);

    /**
     * 某字典下是否已存在 value（同字典内 value 唯一）。
     */
    boolean existsByDictIdAndValue(Long dictId, String value);

    /**
     * 删除某字典下的全部明细（删字典时联动）。
     *
     * @param dictId 字典 ID
     */
    @Modifying
    @Query("delete from DictItem d where d.dictId = ?1")
    void deleteByDictId(Long dictId);
}
