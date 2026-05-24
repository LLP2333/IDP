package com.qvqw.idp.user.internal;

import com.qvqw.idp.user.UserPasswordHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 历史密码 Repository。
 */
public interface UserPasswordHistoryRepository extends JpaRepository<UserPasswordHistory, Long> {

    /**
     * 返回某用户最近的 N 条历史密码（按时间倒序）。
     *
     * @param userId 用户 ID
     * @return 历史记录
     */
    @Query("select h from UserPasswordHistory h where h.userId = :userId order by h.createdAt desc, h.id desc")
    List<UserPasswordHistory> findRecentByUserId(@Param("userId") Long userId);

    /**
     * 清空某用户所有历史密码。
     *
     * @param userId 用户 ID
     */
    @Modifying
    @Query("delete from UserPasswordHistory h where h.userId = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
