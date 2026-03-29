package com.example.rural.exception;

import lombok.Getter;

/**
 * 业务异常
 *
 * 用于在 Service 层抛出明确的业务错误，
 * 会被 GlobalExceptionHandler 捕获并返回给前端
 *
 * 示例: throw new BusinessException(400, "用户名已存在");
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(String message) {
        super(message);
        this.code = 400;
    }
}
