package com.qvqw.idp.message;

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
 * 消息收件箱 / 已读追踪关联表 {@code idp_sys_message_log}。
 *
 * <p>主键 ({@code message_id}, {@code user_id})。{@code read_time} 非空即视为已读。</p>
 */
@Entity
@IdClass(MessageLog.MessageLogId.class)
@Table(name = "idp_sys_message_log", indexes = {
        @Index(name = "idx_idp_sys_message_log_user", columnList = "user_id"),
        @Index(name = "idx_idp_sys_message_log_message", columnList = "message_id")
})
public class MessageLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "message_id", nullable = false)
    private Long messageId;

    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "read_time")
    private LocalDateTime readTime;

    public MessageLog() {
    }

    public MessageLog(Long messageId, Long userId) {
        this.messageId = messageId;
        this.userId = userId;
    }

    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
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
    public static class MessageLogId implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long messageId;
        private Long userId;

        public MessageLogId() {
        }

        public MessageLogId(Long messageId, Long userId) {
            this.messageId = messageId;
            this.userId = userId;
        }

        public Long getMessageId() {
            return messageId;
        }

        public void setMessageId(Long messageId) {
            this.messageId = messageId;
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
            if (!(o instanceof MessageLogId that)) {
                return false;
            }
            return Objects.equals(messageId, that.messageId) && Objects.equals(userId, that.userId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(messageId, userId);
        }
    }
}
