package com.qvqw.idp.notice;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 公告已读追踪表 {@code idp_sys_notice_log}。
 *
 * <p>主键 ({@code notice_id}, {@code user_id})。{@code read_time} 非空即视为已读；
 * 对 {@code noticeScope=ALL} 的公告，未在本表中出现的用户视为未读。</p>
 */
@Entity
@IdClass(NoticeLog.NoticeLogId.class)
@Table(name = "idp_sys_notice_log", indexes = {
        @Index(name = "idx_idp_sys_notice_log_user", columnList = "user_id"),
        @Index(name = "idx_idp_sys_notice_log_notice", columnList = "notice_id")
})
public class NoticeLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "notice_id", nullable = false)
    private Long noticeId;

    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "read_time")
    private LocalDateTime readTime;

    public NoticeLog() {
    }

    public NoticeLog(Long noticeId, Long userId, LocalDateTime readTime) {
        this.noticeId = noticeId;
        this.userId = userId;
        this.readTime = readTime;
    }

    public Long getNoticeId() {
        return noticeId;
    }

    public void setNoticeId(Long noticeId) {
        this.noticeId = noticeId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public LocalDateTime getReadTime() {
        return readTime;
    }

    public void setReadTime(LocalDateTime readTime) {
        this.readTime = readTime;
    }

    /** 联合主键值对象。 */
    public static class NoticeLogId implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long noticeId;
        private Long userId;

        public NoticeLogId() {
        }

        public NoticeLogId(Long noticeId, Long userId) {
            this.noticeId = noticeId;
            this.userId = userId;
        }

        public Long getNoticeId() {
            return noticeId;
        }

        public void setNoticeId(Long noticeId) {
            this.noticeId = noticeId;
        }

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof NoticeLogId that)) {
                return false;
            }
            return Objects.equals(noticeId, that.noticeId) && Objects.equals(userId, that.userId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(noticeId, userId);
        }
    }
}
