package com.qvqw.idp.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户历史密码记录。
 *
 * <p>用于实现 {@code PASSWORD_REPETITION_TIMES} 策略：禁止设置为最近 N 次曾经使用过的密码。
 * 每次修改密码时插入一条新记录，并按用户保留最新 N 条，旧的物理删除。</p>
 */
@Entity
@Table(name = "idp_sys_user_password_history", indexes = {
        @Index(name = "idx_idp_sys_user_pwd_history_user", columnList = "user_id")
})
public class UserPasswordHistory implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public UserPasswordHistory() {
    }

    public UserPasswordHistory(Long userId, String passwordHash) {
        this.userId = userId;
        this.passwordHash = passwordHash;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
