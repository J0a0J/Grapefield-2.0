package com.example.grapefield2.exception;

import com.example.grapefield2.service.DiscordNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final DiscordNotifier discordNotifier;

    // 모든 에러 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleException(
        Exception e,
        HttpServletRequest request
    ) {
        // Security 예외는 Spring Security가 처리하도록 다시 던짐
        if (e instanceof AccessDeniedException) {
            throw (AccessDeniedException) e;
        }
        if (e instanceof AuthenticationException) {
            throw (AuthenticationException) e;
        }

        // 로그
        log.error("[{}] {} - URI: {}, Method: {}, User-Agent: {}",
                determineLevel(e),
                e.getMessage(),
                request.getRequestURI(),
                request.getMethod(),
                request.getHeader("User-Agent"),
                e);

        discordNotifier.sendError(e, request, determineLevel(e));

        // 응답 생성
        return buildErrorResponse(e, request);
    }

    private ResponseEntity<Map<String, String>> buildErrorResponse(Exception e, HttpServletRequest request) {
        Map<String, String> response = Map.of(
                "error", "서버 오류가 발생했습니다.",
                "path", request.getRequestURI(),
                "timestamp", java.time.LocalDateTime.now().toString(),
                "method", request.getMethod(),
                "errorType", e.getClass().getSimpleName()
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }
    
    // 에러 심각도 판단
    private String determineLevel(Exception e) {
        // NullPointerException, IllegalArgumentException 등 → ERROR
        // Connection 관련 → CRITICAL
        
        String className = e.getClass().getSimpleName();
        
        if (className.contains("Connection") || 
            className.contains("Timeout") ||
            className.contains("ServiceUnavailable")) {
            return "CRITICAL";
        }
        
        if (className.contains("NullPointer") || 
            className.contains("IllegalArgument")) {
            return "ERROR";
        }
        
        return "WARN";
    }
}
