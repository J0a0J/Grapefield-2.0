package com.example.grapefield2.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class DiscordNotifier {

    @Value("${discord.webhook.url:}")
    private String webhookUrl;
    
    // 에러 레벨별 색상
    private static final Map<String, Integer> LEVEL_COLORS = Map.of(
        "CRITICAL", 0xFF0000,  // 빨강
        "ERROR", 0xFFA500,     // 주황
        "WARN", 0xFFFF00,      // 노랑
        "INFO", 0x00FF00       // 초록
    );
    
    // 중복 방지 캐시 (간단 버전)
    private final Map<String, ErrorInfo> errorCache = new ConcurrentHashMap<>();
    
    // 기본 에러 알림 (기존 호환)
    public void sendError(Exception e) {
        sendError(e, null, "ERROR");
    }
    
    // 상세 에러 알림
    public void sendError(Exception e, HttpServletRequest request, String level) {

        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.warn("Discord webhook URL이 설정되지 않아 알림을 건너뜁니다.");
            return;
        }
        try {
            // 중복 체크
            String errorKey = generateErrorKey(e);
            ErrorInfo errorInfo = errorCache.computeIfAbsent(errorKey, k -> new ErrorInfo());
            
            // 5분 이내 같은 에러면 카운트만 증가
            if (errorInfo.shouldSkip()) {
                log.info("중복 에러 스킵: {} ({}회)", errorKey, errorInfo.getCount());
                return;
            }
            
            // Embed 메시지 생성
            Map<String, Object> message = createEmbedMessage(e, request, level, errorInfo.getCount());
            
            // Discord 전송
            sendToDiscord(message);
            
            log.info("Discord 알림 전송 완료: {} [{}]", e.getClass().getSimpleName(), level);
            
        } catch (Exception ex) {
            log.error("Discord 알림 실패", ex);
        }
    }
    
    // 시스템 메시지 (서버 시작, 배포 등)
    public void sendSystemMessage(String emoji, String title, String description) {
        try {
            Map<String, Object> message = Map.of(
                "embeds", List.of(Map.of(
                    "title", emoji + " " + title,
                    "description", description,
                    "color", 0x00FF00,
                    "timestamp", LocalDateTime.now().toString()
                ))
            );
            sendToDiscord(message);
        } catch (Exception e) {
            log.error("시스템 메시지 전송 실패", e);
        }
    }
    
    // Embed 메시지 생성
    private Map<String, Object> createEmbedMessage(
        Exception e, 
        HttpServletRequest request, 
        String level,
        int count
    ) {
        List<Map<String, Object>> fields = new ArrayList<>();
        
        // 기본 필드
        fields.add(Map.of(
            "name", "⏰ 시간",
            "value", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
            "inline", true
        ));
        
        fields.add(Map.of(
            "name", "🔢 발생 횟수",
            "value", count + "회",
            "inline", true
        ));
        
        // 요청 정보 (있으면)
        if (request != null) {
            fields.add(Map.of(
                "name", "🌐 요청 URL",
                "value", "`" + request.getMethod() + " " + request.getRequestURI() + "`",
                "inline", false
            ));
            
            fields.add(Map.of(
                "name", "📍 IP 주소",
                "value", maskIp(request.getRemoteAddr()),
                "inline", true
            ));
        }
        
        // 에러 메시지
        fields.add(Map.of(
            "name", "❌ 에러 타입",
            "value", "`" + e.getClass().getSimpleName() + "`",
            "inline", false
        ));
        
        String errorMessage = e.getMessage();
        if (errorMessage != null && !errorMessage.isEmpty()) {
            fields.add(Map.of(
                "name", "💬 메시지",
                "value", errorMessage.length() > 200 
                    ? errorMessage.substring(0, 200) + "..." 
                    : errorMessage,
                "inline", false
            ));
        }
        
        // 스택 트레이스 (핵심만)
        String stack = getTopStackTrace(e, 5);
        if (!stack.isEmpty()) {
            fields.add(Map.of(
                "name", "📚 스택 트레이스",
                "value", "```" + stack + "```",
                "inline", false
            ));
        }
        
        return Map.of(
            "embeds", List.of(Map.of(
                "title", getLevelEmoji(level) + " " + level + " 에러 발생",
                "color", LEVEL_COLORS.getOrDefault(level, 0xFFA500),
                "fields", fields,
                "footer", Map.of(
                    "text", "Grapefield Monitor v1.0"
                ),
                "timestamp", LocalDateTime.now().toString()
            ))
        );
    }
    
    // Discord Webhook 전송
    private void sendToDiscord(Map<String, Object> message) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(message, headers);
        restTemplate.postForObject(webhookUrl, entity, String.class);
    }
    
    // 에러 키 생성 (중복 판별용)
    private String generateErrorKey(Exception e) {
        return e.getClass().getName() + ":" + 
               (e.getMessage() != null ? e.getMessage() : "null");
    }
    
    // IP 마스킹
    private String maskIp(String ip) {
        if (ip == null) return "Unknown";
        String[] parts = ip.split("\\.");
        if (parts.length == 4) {
            return parts[0] + "." + parts[1] + ".***.***";
        }
        return ip;
    }
    
    // 스택 트레이스 상위 N개만 추출
    private String getTopStackTrace(Exception e, int lines) {
        StackTraceElement[] trace = e.getStackTrace();
        StringBuilder sb = new StringBuilder();
        
        int limit = Math.min(lines, trace.length);
        for (int i = 0; i < limit; i++) {
            sb.append(trace[i].toString()).append("\n");
        }
        
        return sb.toString();
    }
    
    // 레벨별 이모지
    private String getLevelEmoji(String level) {
        return switch (level) {
            case "CRITICAL" -> "🚨";
            case "ERROR" -> "⚠️";
            case "WARN" -> "⚡";
            case "INFO" -> "ℹ️";
            default -> "❓";
        };
    }
    
    // 에러 정보 클래스 (중복 체크용)
    private static class ErrorInfo {
        private int count = 0;
        private long lastSent = 0;
        private static final long COOLDOWN = 5 * 60 * 1000; // 5분
        
        public synchronized boolean shouldSkip() {
            count++;
            long now = System.currentTimeMillis();
            
            if (now - lastSent < COOLDOWN) {
                return true; // 스킵
            }
            
            lastSent = now;
            return false;
        }

        public int getCount() {
            return count;
        }
    }
}
