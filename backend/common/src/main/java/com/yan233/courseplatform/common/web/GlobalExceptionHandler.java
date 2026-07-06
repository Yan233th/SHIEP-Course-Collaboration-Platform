package com.yan233.courseplatform.common.web;

import com.yan233.courseplatform.common.api.Result;
import com.yan233.courseplatform.common.exception.BusinessException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusiness(BusinessException ex) {
        return Result.fail(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            BindException.class,
            ConstraintViolationException.class,
            HttpMessageNotReadableException.class
    })
    public Result<Void> handleValidation(Exception ex) {
        return Result.fail(400, "参数校验失败: " + ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleOther(Exception ex) {
        return Result.fail(500, "服务器内部错误: " + ex.getMessage());
    }
}

