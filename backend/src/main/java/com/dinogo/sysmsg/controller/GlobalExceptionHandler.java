package com.dinogo.sysmsg.controller;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientResponseException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String,Object>> validation(MethodArgumentNotValidException ex,HttpServletRequest req){
        String message=ex.getBindingResult().getFieldErrors().stream().map(e->e.getField()+": "+e.getDefaultMessage()).collect(Collectors.joining("; "));
        return body(HttpStatus.BAD_REQUEST,message,req);
    }
    @ExceptionHandler({IllegalArgumentException.class,MissingRequestHeaderException.class})
    public ResponseEntity<Map<String,Object>> badRequest(Exception ex,HttpServletRequest req){return body(HttpStatus.BAD_REQUEST,ex.getMessage(),req);}
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String,Object>> forbidden(SecurityException ex,HttpServletRequest req){return body(HttpStatus.FORBIDDEN,ex.getMessage(),req);}
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String,Object>> conflict(IllegalStateException ex,HttpServletRequest req){return body(HttpStatus.CONFLICT,ex.getMessage(),req);}
    @ExceptionHandler(RestClientResponseException.class)
    public ResponseEntity<Map<String,Object>> upstream(RestClientResponseException ex,HttpServletRequest req){return body(HttpStatus.BAD_GATEWAY,"外部模組 API 呼叫失敗："+ex.getStatusCode(),req);}
    private ResponseEntity<Map<String,Object>> body(HttpStatus status,String message,HttpServletRequest req){
        Map<String,Object> result=new LinkedHashMap<>();result.put("timestamp",LocalDateTime.now());result.put("status",status.value());result.put("error",status.getReasonPhrase());result.put("message",message);result.put("path",req.getRequestURI());return ResponseEntity.status(status).body(result);
    }
}
