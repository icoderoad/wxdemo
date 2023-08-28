package com.icoderoad.example.demo.exception;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RateLimitException.class)
    public String handleRateLimitException(RateLimitException ex) {
        return ex.getMessage(); // 返回自定义错误信息
    }
}