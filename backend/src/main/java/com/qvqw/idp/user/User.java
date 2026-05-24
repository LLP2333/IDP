package com.qvqw.idp.user;

import com.qvqw.idp.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * 用户实体。
 */
@Entity
@Table(name = "idp_sys_user", uniqueConstraints = {
        @UniqueConstraint(name = "uk_idp_sys_user_username", columnNames = "username")
})
public class User extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, length = 64)
    private String username;

    /** BCrypt 加密后的密码 */
    @Column(name = "password", nullable = false, length = 100)
    private String password;

    @Column(name = "nickname", length = 64)
    private String nickname;

    @Column(name = "email", length = 128)
    private String email;

    @Column(name = "phone", length = 32)
    private String phone;

    /** 0=未知, 1=男, 2=女 */
    @Column(name = "gender", nullable = false)
    private Integer gender = 0;

    @Column(name = "avatar", length = 255)
    private String avatar;

    @Column(name = "description", length = 255)
    private String description;

    /** 1=启用, 0=禁用 */
    @Column(name = "status", nullable = false)
    private Integer status = 1;

    @Column(name = "is_system", nullable = false)
    private Boolean isSystem = false;

    @Column(name = "pwd_reset_at")
    private LocalDateTime pwdResetAt;

    /** 当前累计的连续登录失败次数，登录成功或锁定到期后清零。 */
    @Column(name = "pwd_error_count", nullable = false)
    private Integer pwdErrorCount = 0;

    /** 账号锁定到期时间；为空或早于当前时间表示未锁定。 */
    @Column(name = "pwd_locked_until")
    private LocalDateTime pwdLockedUntil;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Integer getGender() {
        return gender;
    }

    public void setGender(Integer gender) {
        this.gender = gender;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Boolean getIsSystem() {
        return isSystem;
    }

    public void setIsSystem(Boolean isSystem) {
        this.isSystem = isSystem;
    }

    public LocalDateTime getPwdResetAt() {
        return pwdResetAt;
    }

    public void setPwdResetAt(LocalDateTime pwdResetAt) {
        this.pwdResetAt = pwdResetAt;
    }

    public Integer getPwdErrorCount() {
        return pwdErrorCount;
    }

    public void setPwdErrorCount(Integer pwdErrorCount) {
        this.pwdErrorCount = pwdErrorCount;
    }

    public LocalDateTime getPwdLockedUntil() {
        return pwdLockedUntil;
    }

    public void setPwdLockedUntil(LocalDateTime pwdLockedUntil) {
        this.pwdLockedUntil = pwdLockedUntil;
    }
}
