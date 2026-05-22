package com.iccuu.general_web_backend.common.handler;

import com.iccuu.general_web_backend.common.constant.ErrorCode;
import com.iccuu.general_web_backend.common.exception.AuthenticationException;
import com.iccuu.general_web_backend.common.exception.BusinessException;
import com.iccuu.general_web_backend.common.result.FieldError;
import com.iccuu.general_web_backend.common.result.R;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<R<Void>> handleBusinessException(BusinessException e) {
        int httpStatus = errorCodeToHttpStatus(e.getCode());
        log.warn("Business exception: code={}, message={}", e.getCode(), e.getMessage());
        return ResponseEntity.status(httpStatus)
                .body(R.fail(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<R<List<FieldError>>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        List<FieldError> errors = new java.util.ArrayList<>();
        ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldError(fe.getField(), fe.getDefaultMessage()))
                .forEach(errors::add);
        ex.getBindingResult().getGlobalErrors().stream()
                .map(ge -> new FieldError(ge.getObjectName(), ge.getDefaultMessage()))
                .forEach(errors::add);
        log.warn("Validation failed: {}", errors);
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(R.fail(ErrorCode.VALIDATION_ERROR.getCode(), "参数校验失败", errors));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<R<List<FieldError>>> handleBindException(BindException ex) {
        List<FieldError> errors = new java.util.ArrayList<>();
        ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldError(fe.getField(), fe.getDefaultMessage()))
                .forEach(errors::add);
        ex.getBindingResult().getGlobalErrors().stream()
                .map(ge -> new FieldError(ge.getObjectName(), ge.getDefaultMessage()))
                .forEach(errors::add);
        log.warn("Bind validation failed: {}", errors);
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(R.fail(ErrorCode.VALIDATION_ERROR.getCode(), "参数校验失败", errors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<R<List<FieldError>>> handleConstraintViolation(ConstraintViolationException ex) {
        List<FieldError> errors = ex.getConstraintViolations().stream()
                .map(cv -> new FieldError(getPropertyPath(cv), cv.getMessage()))
                .toList();
        log.warn("Constraint violation: {}", errors);
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(R.fail(ErrorCode.VALIDATION_ERROR.getCode(), "参数校验失败", errors));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<R<Void>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(R.fail(403, ex.getMessage() != null ? ex.getMessage() : "权限不足"));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<R<Void>> handleAuthenticationException(AuthenticationException ex) {
        log.warn("Authentication exception: code={}, message={}", ex.getCode(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(R.fail(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<R<Void>> handleMethodNotSupported(
            org.springframework.web.HttpRequestMethodNotSupportedException ex) {
        log.warn("Method not allowed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(R.fail(405, "Method " + ex.getMethod() + " not supported for this endpoint"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<R<Void>> handleException(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(R.fail(ErrorCode.INTERNAL_ERROR.getCode(),
                        ErrorCode.INTERNAL_ERROR.getMessageKey()));
    }

    private int errorCodeToHttpStatus(int code) {
        if (code >= 50000) {
            return HttpStatus.INTERNAL_SERVER_ERROR.value();
        }
        if (code >= 40000) {
            return HttpStatus.BAD_REQUEST.value();
        }
        if (code >= 11000) {
            return HttpStatus.UNPROCESSABLE_ENTITY.value();
        }
        if (code >= 10000) {
            return HttpStatus.UNAUTHORIZED.value();
        }
        return HttpStatus.INTERNAL_SERVER_ERROR.value();
    }

    private String getPropertyPath(ConstraintViolation<?> cv) {
        String path = cv.getPropertyPath().toString();
        int dotIndex = path.lastIndexOf('.');
        return dotIndex >= 0 ? path.substring(dotIndex + 1) : path;
    }
}
