package com.amr.dashboard.config;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.stream.Collectors;

/**
 * RFC 7807 Problem Detail 표준 에러 응답
 * 모든 API 에러를 일관된 JSON 포맷으로 반환:
 * { "type", "title", "status", "detail", "instance" }
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException e, HttpServletResponse response) {
        log.warn("[API Error] Bad Request: {}", e.getMessage());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
        pd.setType(URI.create("/errors/bad-request"));
        pd.setTitle("Bad Request");
        return pd;
    }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleIllegalState(IllegalStateException e, HttpServletResponse response) {
        log.warn("[API Error] Conflict: {}", e.getMessage());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
        pd.setType(URI.create("/errors/conflict"));
        pd.setTitle("Conflict");
        return pd;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException e, HttpServletResponse response) {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.");
        pd.setType(URI.create("/errors/forbidden"));
        pd.setTitle("Forbidden");
        return pd;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException e, HttpServletResponse response) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("[API Error] Validation: {}", detail);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        pd.setType(URI.create("/errors/validation"));
        pd.setTitle("Validation Failed");
        return pd;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneral(Exception e, HttpServletResponse response) {
        log.error("[API Error] Internal Server Error", e);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "서버 내부 오류가 발생했습니다.");
        pd.setType(URI.create("/errors/internal"));
        pd.setTitle("Internal Server Error");
        return pd;
    }
}
