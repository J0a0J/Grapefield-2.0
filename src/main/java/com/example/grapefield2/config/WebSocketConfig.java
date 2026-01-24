package com.example.grapefield2.config;

import com.example.grapefield2.entity.User;
import com.example.grapefield2.repository.UserRepository;
import com.example.grapefield2.service.ChatSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;


@Slf4j
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final ChatSessionService chatSessionService;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {

        // 메시지 브로커 설정 (서버 -> 클라이언트)
        config.enableSimpleBroker("/topic");

        // 클라이언트 -> 서버 prefix
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 웹소켓 엔드포인트
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(
                "http://localhost:5173",  // Vue 개발서버
                "http://localhost:8080"  // 백엔드 개발서버

        );
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
                        message, StompHeaderAccessor.class);

                if (accessor == null) {
                    return message;
                }

                // CONNECT : JWT 검증
                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    handleConnect(accessor);
                } else if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
                    handleDisconnect(accessor);
                }

                return message;
            }
        });
    }

    // 연결 시 JWT 검증
    private void handleConnect(StompHeaderAccessor accessor) {
        try {
            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.error("Authorization 헤더 없음");
                throw new IllegalArgumentException("인증 토큰이 없습니다.");
            }

            String token = authHeader.substring(7);

            if (!jwtTokenProvider.validateToken(token)) {
                log.error("유효하지 않은 토큰");
                throw new IllegalArgumentException("유효하지 않은 토큰입니다");
            }

            String email = jwtTokenProvider.getEmail(token);
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

            // 세션에 사용자 정보 저장
            accessor.getSessionAttributes().put("userId", user.getIdx());
            accessor.getSessionAttributes().put("username", user.getUsername());

            log.info("WebSocket 연결 성공: userId={}, username={}", user.getIdx(), user.getUsername());
        } catch (Exception e) {
            log.info("WebSocket 인증 실패: ", e);
            throw new IllegalArgumentException("인증 실패");
        }
    }

    private void handleDisconnect(StompHeaderAccessor accessor) {
        try {
            String sessionId = accessor.getSessionId();
            Long userId = (Long) accessor.getSessionAttributes().get("userId");
            String username = (String) accessor.getSessionAttributes().get("username");
            String category = (String) accessor.getSessionAttributes().get("category");

            if (sessionId == null || category == null) {
                log.warn("퇴장 처리: 세션 정보 없음");
                return;
            }

            log.info("퇴장 처리 시작: userId={}, username={}, category={}", userId, username, category);

            chatSessionService.removeSession(category, sessionId);

            long userCount = chatSessionService.getUserCount(category);

            log.info("퇴장 완료: userId={}, category={}, 인원={}", userId, category, userCount);
        } catch (Exception e) {
            log.error("퇴장 처리 실패: ", e);
        }
    }
}
