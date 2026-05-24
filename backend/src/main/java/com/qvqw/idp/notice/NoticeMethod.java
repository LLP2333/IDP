package com.qvqw.idp.notice;

/**
 * 公告通知方式。
 *
 * <p>整数值与 {@code notice_method_enum} 字典明细的 value 保持一致。</p>
 */
public enum NoticeMethod {

    /** 系统消息（推送到收件箱） */
    SYSTEM_MESSAGE(1),

    /** 登录弹窗（下次登录强制查看） */
    POPUP(2);

    private final Integer value;

    NoticeMethod(Integer value) {
        this.value = value;
    }

    public Integer getValue() {
        return value;
    }
}
