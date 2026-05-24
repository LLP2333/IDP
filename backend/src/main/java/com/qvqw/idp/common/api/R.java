package com.qvqw.idp.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;

/**
 * 统一响应结构。
 *
 * <p>所有 Controller 返回都包裹在 {@code R<T>} 中：{@code code=0} 表示成功，
 * 否则视为业务错误，前端只展示 {@code msg}。</p>
 *
 * @param <T> 响应数据类型
 */
@Schema(description = "统一响应结构")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class R<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 业务成功状态码。 */
    public static final int CODE_SUCCESS = 0;
    /** 通用业务失败状态码。 */
    public static final int CODE_FAIL = 500;
    /** 未登录或登录过期。 */
    public static final int CODE_UNAUTHORIZED = 401;
    /** 已登录但无权限。 */
    public static final int CODE_FORBIDDEN = 403;

    @Schema(description = "业务状态码，0=成功", example = "0")
    private int code;

    @Schema(description = "业务消息", example = "success")
    private String msg;

    @Schema(description = "业务数据；成功时为接口数据，失败时通常为 null")
    private T data;

    @Schema(description = "服务端响应时间戳（毫秒）", example = "1716522000000")
    private long timestamp;

    public R() {
        this.timestamp = System.currentTimeMillis();
    }

    public R(int code, String msg, T data) {
        this();
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    /**
     * 构造一个不带数据的成功响应。
     *
     * @param <T> 数据类型占位
     * @return code=0、data=null 的成功响应
     */
    public static <T> R<T> ok() {
        return new R<>(CODE_SUCCESS, "success", null);
    }

    /**
     * 构造一个带数据的成功响应。
     *
     * @param data 响应数据
     * @param <T>  数据类型
     * @return code=0、data=指定值 的成功响应
     */
    public static <T> R<T> ok(T data) {
        return new R<>(CODE_SUCCESS, "success", data);
    }

    /**
     * 构造默认 500 的失败响应。
     *
     * @param msg 错误消息
     * @param <T> 数据类型占位
     * @return 失败响应
     */
    public static <T> R<T> fail(String msg) {
        return new R<>(CODE_FAIL, msg, null);
    }

    /**
     * 构造指定 code 的失败响应（401/403/自定义业务错误等）。
     *
     * @param code 业务状态码
     * @param msg  错误消息
     * @param <T>  数据类型占位
     * @return 失败响应
     */
    public static <T> R<T> fail(int code, String msg) {
        return new R<>(code, msg, null);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
