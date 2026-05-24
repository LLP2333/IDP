package com.qvqw.idp.common.exception;

import com.qvqw.idp.common.api.R;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理。
 *
 * <p>统一把异常转换成 {@link R} 形态的 JSON：业务异常 → 400 + 自定义 code；
 * 校验类异常 → 400；认证 / 鉴权类 → 401 / 403；其余兜底 500。</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 业务可预期异常：通过 {@link BusinessException#getCode()} 透传业务码。
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<R<Void>> handleBusiness(BusinessException ex) {
        log.warn("[业务异常] {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(R.fail(ex.getCode(), ex.getMessage()));
    }

    /**
     * {@code @RequestBody} 上的 Bean Validation 异常，拼接所有字段错误消息。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<R<Void>> handleValid(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(R.fail(400, msg));
    }

    /**
     * 表单 / Query 参数的绑定异常。
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<R<Void>> handleBind(BindException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(R.fail(400, msg));
    }

    /**
     * {@code @Validated} 在 Controller 方法参数上抛出的约束违反异常。
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<R<Void>> handleConstraint(ConstraintViolationException ex) {
        String msg = ex.getConstraintViolations().stream()
                .map(v -> v.getMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(R.fail(400, msg));
    }

    /**
     * Spring Security 未认证异常：返回 401。
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<R<Void>> handleAuthentication(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(R.fail(R.CODE_UNAUTHORIZED, "未登录或登录已过期"));
    }

    /**
     * Spring Security 拒绝访问异常：返回 403。
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<R<Void>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(R.fail(R.CODE_FORBIDDEN, "无访问权限"));
    }

    /**
     * 其余未捕获异常的兜底处理：记录详细堆栈，但不向前端暴露细节。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<R<Void>> handleGeneric(Exception ex) {
        log.error("[系统异常]", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(R.fail("系统繁忙，请稍后再试"));
    }
}
