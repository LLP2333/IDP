package com.qvqw.idp.notice;

/**
 * 公告状态。
 *
 * <p>整数值与 {@code notice_status_enum} 字典明细的 value 保持一致。</p>
 */
public enum NoticeStatus {

    /** 草稿（创作中，不发布） */
    DRAFT(1),

    /** 待发布（已设定定时，等待 NoticeScheduler 触发） */
    PENDING(2),

    /** 已发布（对收件人可见） */
    PUBLISHED(3);

    private final Integer value;

    NoticeStatus(Integer value) {
        this.value = value;
    }

    public Integer getValue() {
        return value;
    }

    public static NoticeStatus of(Integer value) {
        if (value == null) {
            return null;
        }
        for (NoticeStatus s : values()) {
            if (s.value.equals(value)) {
                return s;
            }
        }
        return null;
    }
}
