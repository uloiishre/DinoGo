package com.dinogo.review.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;

/** 將評論模組的業務、驗證、併發及外部 API 例外轉成一致的 HTTP 錯誤格式。 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException exception) {

        Map<String, String> fields = new LinkedHashMap<>();
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            fields.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        return response(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "輸入資料驗證失敗", fields);
    }

    @ExceptionHandler(ReviewNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ReviewNotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, "REVIEW_NOT_FOUND", exception.getMessage(), null);
    }

    @ExceptionHandler(ReviewForbiddenException.class)
    public ResponseEntity<Map<String, Object>> handleForbidden(ReviewForbiddenException exception) {
        return response(HttpStatus.FORBIDDEN, "REVIEW_FORBIDDEN", exception.getMessage(), null);
    }

    @ExceptionHandler({ReviewConflictException.class, OptimisticLockingFailureException.class})
    public ResponseEntity<Map<String, Object>> handleConflict(Exception exception) {
        // 第二個重複建立請求或較舊版本的 UPDATE 會收到 HTTP 409。
        return response(HttpStatus.CONFLICT, "REVIEW_CONFLICT", exception.getMessage(), null);
    }

    @ExceptionHandler({InvalidOrderStateException.class, IllegalArgumentException.class})
    public ResponseEntity<Map<String, Object>> handleBadRequest(RuntimeException exception) {
        return response(HttpStatus.BAD_REQUEST, "BAD_REQUEST", exception.getMessage(), null);
    }

    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<Map<String, Object>> handleExternalApi(RestClientException exception) {
        return response(
                HttpStatus.BAD_GATEWAY,
                "EXTERNAL_API_ERROR",
                "其他模組 API 呼叫失敗",
                null);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidExternalData(
            IllegalStateException exception) {
        return response(HttpStatus.BAD_GATEWAY, "EXTERNAL_DATA_INVALID", exception.getMessage(), null);
    }

    private ResponseEntity<Map<String, Object>> response(
            HttpStatus status,
            String error,
            String message,
            Object details) {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status.value());
        body.put("error", error);
        body.put("message", message);
        if (details != null) {
            body.put("details", details);
        }
        return ResponseEntity.status(status).body(body);
    }
}
