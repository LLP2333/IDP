package com.qvqw.idp.common.exception;

import java.io.Serial;

/**
 * 业务异常。
 *
 * <p>所有可预期的、需要返回给前端展示的异常统一抛出该类型；由 {@link GlobalExceptionHandler}
 * 统一转换为 {@link com.qvqw.idp.common.api.R} 结构的 4xx 响应。</p>
 */
public class BusinessException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final int code;

    /**
     * 使用默认业务码 500 构造异常。
     *
     * @param message 错误描述（直接展示给用户）
     */
    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }

    /**
     * 使用自定义业务码构造异常（如 401、403 等）。
     *
     * @param code    业务码
     * @param message 错误描述
     */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * @return 业务码（用于前端针对不同 code 做差异化处理）
     */
    public int getCode() {
        return code;
    }
}
