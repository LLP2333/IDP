package com.qvqw.idp.message;

import com.qvqw.idp.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.io.Serial;

/**
 * 站内消息实体。
 *
 * <p>消息本体只承载 “一份内容”；每个收件人单独一条 {@link MessageLog} 记录读状态。</p>
 *
 * <p>{@code type} 当前固定为 {@code 1=系统消息}，后续如需通知 / 广播等不同来源再扩展枚举。</p>
 */
@Entity
@Table(name = "idp_sys_message")
public class Message extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 消息类型；当前 1=系统消息。 */
    @Column(name = "type", nullable = false)
    private Integer type = 1;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "text")
    private String content;

    /** 业务跳转路径（前端点击消息时跳过去）。 */
    @Column(name = "path", length = 255)
    private String path;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
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

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
