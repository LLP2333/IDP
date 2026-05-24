package com.qvqw.idp.message.internal;

import com.qvqw.idp.message.MessageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 消息收件箱 / 已读状态 JPA Repository。
 */
public interface MessageLogRepository extends JpaRepository<MessageLog, MessageLog.MessageLogId> {

    /**
     * 取某用户的某条消息已读状态记录。
     *
     * @param userId    用户 ID
     * @param messageId 消息 ID
     * @return 收件箱记录
     */
    Optional<MessageLog> findByUserIdAndMessageId(Long userId, Long messageId);

    /**
     * 当前用户的未读条数。
     *
     * @param userId 用户 ID
     * @return 未读数量
     */
    long countByUserIdAndReadTimeIsNull(Long userId);

    /**
     * 批量将当前用户所有未读消息标记为已读。
     *
     * @param userId   用户 ID
     * @param readTime 已读时间
     * @return 受影响行数
     */
    @Modifying
    @Query("update MessageLog m set m.readTime = :readTime where m.userId = :userId and m.readTime is null")
    int markAllReadByUserId(@Param("userId") Long userId, @Param("readTime") LocalDateTime readTime);

    /**
     * 删除某条消息的所有收件箱记录（消息主表被删时联动）。
     *
     * @param messageId 消息 ID
     */
    @Modifying
    @Query("delete from MessageLog m where m.messageId = :messageId")
    void deleteByMessageId(@Param("messageId") Long messageId);
}
