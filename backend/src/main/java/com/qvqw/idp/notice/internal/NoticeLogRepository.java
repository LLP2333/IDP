package com.qvqw.idp.notice.internal;

import com.qvqw.idp.notice.NoticeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 公告日志 / 已读状态 JPA Repository。
 */
public interface NoticeLogRepository extends JpaRepository<NoticeLog, NoticeLog.NoticeLogId> {

    /**
     * 取某用户对某公告的阅读记录。
     */
    Optional<NoticeLog> findByUserIdAndNoticeId(Long userId, Long noticeId);

    /**
     * 用户读过的公告 ID 列表（用于批量计算 isRead）。
     *
     * @param userId    用户 ID
     * @param noticeIds 公告 ID 列表
     * @return 已有日志的公告 ID
     */
    @Query("select l.noticeId from NoticeLog l where l.userId = :uid and l.noticeId in :ids and l.readTime is not null")
    List<Long> findReadNoticeIdsByUser(@Param("uid") Long userId, @Param("ids") Collection<Long> noticeIds);

    /**
     * 批量删除某些公告的已读记录（公告被删除时联动）。
     *
     * @param noticeIds 公告 ID 列表
     */
    @Modifying
    @Query("delete from NoticeLog l where l.noticeId in :ids")
    void deleteByNoticeIds(@Param("ids") Collection<Long> noticeIds);
}
