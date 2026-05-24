package com.qvqw.idp.option.internal;

import com.qvqw.idp.option.OptionCategory;
import com.qvqw.idp.option.SystemOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * {@link SystemOption} 的 JPA Repository。
 */
public interface OptionRepository extends JpaRepository<SystemOption, Long> {

    /**
     * 按类别列出（按 ID 升序，避免随机顺序）。
     *
     * @param category 类别
     * @return 该类别下所有配置
     */
    List<SystemOption> findByCategoryOrderByIdAsc(OptionCategory category);

    /**
     * 按 code 集合查询。
     *
     * @param codes 键集合
     * @return 命中记录
     */
    List<SystemOption> findByCodeIn(List<String> codes);

    /**
     * 按 code 精确查找。
     *
     * @param code 键
     * @return 单条记录
     */
    Optional<SystemOption> findByCode(String code);

    /**
     * 把某类别下所有参数的 {@code value} 设置为 {@code null}（视为恢复默认）。
     *
     * @param category 类别
     * @return 受影响行数
     */
    @Modifying
    @Query("update SystemOption o set o.value = null where o.category = :category")
    int resetByCategory(@Param("category") OptionCategory category);

    /**
     * 把指定 code 列表的 {@code value} 设置为 {@code null}。
     *
     * @param codes 键列表
     * @return 受影响行数
     */
    @Modifying
    @Query("update SystemOption o set o.value = null where o.code in :codes")
    int resetByCodes(@Param("codes") List<String> codes);

    /**
     * 按 code 是否存在。
     *
     * @param code 键
     * @return 存在返回 {@code true}
     */
    boolean existsByCode(String code);
}
