package com.qvqw.idp.common.exception;

import java.io.Serial;

/**
 * 业务异常
 *
 * <p>所有可预期的、需要返回给前端展示的异常统一抛出该类型。</p>
 */
public class BusinessException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final int code;

    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
