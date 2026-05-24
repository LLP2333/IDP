package com.qvqw.idp.notice;

/**
 * 公告通知范围。
 *
 * <p>整数值与 {@code notice_scope_enum} 字典明细的 value 保持一致。</p>
 */
public enum NoticeScope {

    /** 所有人 */
    ALL(1),

    /** 指定用户 */
    USER(2);

    private final Integer value;

    NoticeScope(Integer value) {
        this.value = value;
    }

    public Integer getValue() {
        return value;
    }

    /**
     * 按整数值反查枚举。
     *
     * @param value 整数值
     * @return 对应枚举；不存在时返回 {@code null}
     */
    public static NoticeScope of(Integer value) {
        if (value == null) {
            return null;
        }
        for (NoticeScope s : values()) {
            if (s.value.equals(value)) {
                return s;
            }
        }
        return null;
    }
}
