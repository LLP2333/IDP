package com.qvqw.idp.notice;

import com.qvqw.idp.common.persistence.BaseEntity;
import com.qvqw.idp.notice.internal.IntegerListJsonConverter;
import com.qvqw.idp.notice.internal.LongListJsonConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.io.Serial;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 公告实体。
 *
 * <p>{@code noticeUsers} / {@code noticeMethods} 通过 Jackson 序列化为 JSON 字符串写入
 * TEXT 列，跨 PostgreSQL / H2 一致；如有大规模查询需求再切换到 PostgreSQL 原生 jsonb。</p>
 */
@Entity
@Table(name = "idp_sys_notice", indexes = {
        @Index(name = "idx_idp_sys_notice_status", columnList = "status"),
        @Index(name = "idx_idp_sys_notice_publish", columnList = "publish_time"),
        @Index(name = "idx_idp_sys_notice_top", columnList = "is_top")
})
public class Notice extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "text")
    private String content;

    /** 分类，取值于字典 {@code notice_type}。 */
    @Column(name = "type", nullable = false, length = 30)
    private String type;

    /** 通知范围；存储整数值。 */
    @Column(name = "notice_scope", nullable = false)
    private Integer noticeScope;

    /** 通知用户 ID 列表（JSON 序列化为 TEXT 列）。 */
    @Convert(converter = LongListJsonConverter.class)
    @Column(name = "notice_users", columnDefinition = "text")
    private List<Long> noticeUsers;

    /** 通知方式整数列表（JSON 序列化为 TEXT 列）。 */
    @Convert(converter = IntegerListJsonConverter.class)
    @Column(name = "notice_methods", columnDefinition = "text")
    private List<Integer> noticeMethods;

    @Column(name = "is_timing", nullable = false)
    private Boolean isTiming = false;

    @Column(name = "publish_time")
    private LocalDateTime publishTime;

    @Column(name = "is_top", nullable = false)
    private Boolean isTop = false;

    /** 状态，整数值。 */
    @Column(name = "status", nullable = false)
    private Integer status;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getNoticeScope() {
        return noticeScope;
    }

    public void setNoticeScope(Integer noticeScope) {
        this.noticeScope = noticeScope;
    }

    public List<Long> getNoticeUsers() {
        return noticeUsers;
    }

    public void setNoticeUsers(List<Long> noticeUsers) {
        this.noticeUsers = noticeUsers;
    }

    public List<Integer> getNoticeMethods() {
        return noticeMethods;
    }

    public void setNoticeMethods(List<Integer> noticeMethods) {
        this.noticeMethods = noticeMethods;
    }

    public Boolean getIsTiming() {
        return isTiming;
    }

    public void setIsTiming(Boolean isTiming) {
        this.isTiming = isTiming;
    }

    public LocalDateTime getPublishTime() {
        return publishTime;
    }

    public void setPublishTime(LocalDateTime publishTime) {
        this.publishTime = publishTime;
    }

    public Boolean getIsTop() {
        return isTop;
    }

    public void setIsTop(Boolean isTop) {
        this.isTop = isTop;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
