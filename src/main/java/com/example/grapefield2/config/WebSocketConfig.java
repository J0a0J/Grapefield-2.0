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

import javax.annotation.Nullable;
import java.util.Map;

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
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(
                        "http://localhost:5173",
                        "http://localhost:8080",
                        "http://grapefield-2.kro.kr",
                        "https://grapefield-2.kro.kr"
                );
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(@Nullable Message<?> message, @Nullable MessageChannel channel) {
                if (message == null) return null;

                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
                        message, StompHeaderAccessor.class);

                if (accessor != null) {
                    if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                        handleConnect(accessor);
                    } else if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
                        handleDisconnect(accessor);
                    }
                }

                return message;
            }
        });
    }

    private void handleConnect(StompHeaderAccessor accessor) {
        try {
            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new IllegalArgumentException("인증 토큰이 없습니다.");
            }

            String token = authHeader.substring(7);

            if (!jwtTokenProvider.validateToken(token)) {
                throw new IllegalArgumentException("유효하지 않은 토큰입니다");
            }

            String email = jwtTokenProvider.getEmail(token);
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
            if (sessionAttributes != null) {
                accessor.getSessionAttributes().put("userId", user.getIdx());
                accessor.getSessionAttributes().put("username", user.getUsername());
            }

            log.info("WebSocket 연결: userId={}", user.getIdx());
        } catch (Exception e) {
            log.error("WebSocket 인증 실패: {}", e.getMessage());
            throw new IllegalArgumentException("인증 실패");
        }
    }

    private void handleDisconnect(StompHeaderAccessor accessor) {
        try {
            String sessionId = accessor.getSessionId();
            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();

            if (sessionAttributes != null) {
                String category = (String) sessionAttributes.get("category");

                if (sessionId != null && category != null) {
                    chatSessionService.removeSession(category, sessionId);
                    log.info("WebSocket 퇴장: category={}", category);
                }
            }
        } catch (Exception e) {
            log.error("퇴장 처리 실패: {}", e.getMessage());
        }
    }
}