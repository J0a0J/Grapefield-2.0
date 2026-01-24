package com.example.grapefield2.config;

import com.example.grapefield2.service.ChatSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final ChatSessionService chatSessionService;

    // 웹소켓 연결 이벤트
    // 클라이언트가 connect 프레임 보내면 자동 실행
    @EventListener
    public void handleWebSocketConnectionListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        log.info("[WebSocket 연결] 세션 ID: {}", sessionId);
    }

    // 웹소켓 연결 해제, 브라우저 닫기, 네트워크 끊김 등
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        String category = (String) headerAccessor.getSessionAttributes().get("category");

        if (category != null) {
            chatSessionService.removeSession(category, sessionId);
            log.info("[WebSocket 퇴장] 세션: {}, 채팅방: {}", sessionId, category);
        } else {
            log.info("[WebSocket 연결 해제] 세션: {} (채팅방 정보 없음)", sessionId);
        }
    }
}
