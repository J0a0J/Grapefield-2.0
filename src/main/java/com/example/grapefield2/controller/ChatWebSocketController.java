package com.example.grapefield2.controller;

import com.example.grapefield2.dto.*;
import com.example.grapefield2.entity.ChatMessage;
import com.example.grapefield2.entity.ChatRoom;
import com.example.grapefield2.entity.User;
import com.example.grapefield2.repository.ChatMessageRepository;
import com.example.grapefield2.repository.ChatRoomRepository;
import com.example.grapefield2.repository.UserRepository;
import com.example.grapefield2.service.ChatService;
import com.example.grapefield2.service.ChatSessionService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final ChatSessionService chatSessionService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;

    // 채팅방 입장
    @MessageMapping("/chat.enter")
    public void enter(
            @Payload EnterRequest request,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        try {
            String sessionId = headerAccessor.getSessionId();
            String category = request.getCategory();

            Long userId = (Long) headerAccessor.getSessionAttributes().get("userId");
            String username = (String) headerAccessor.getSessionAttributes().get("username");

            log.info("채팅방 입장 요청: userId={}, username={}, category={}", userId, username, request.getCategory());

            if (userId == null || username == null) {
                throw new IllegalArgumentException("인증되지 않은 사용자");
            }

            log.info("[입장] 사용자 ID: {}, 닉네임: {}, 채팅방: {}", userId, username, category);

            // 세션 속에서 채팅방 정보 저장 (나중에 disconnect 시 사용)
            headerAccessor.getSessionAttributes().put("category", category);

            User user = userRepository.findByIdx(userId)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

            // Redis에 세션 추가
            chatSessionService.addSession(category, sessionId);
            // 현재 접속자 수
            long userCount = chatSessionService.getUserCount(category);

            // 입장 메시지 브로드캐스트
            EnterMessage enterMessage = EnterMessage.builder()
                    .type("ENTER")
                    .userId(userId)
                    .username(username)
                    .userCount(userCount)
                    .build();

            messagingTemplate.convertAndSend("/topic/room/" + category, enterMessage);

            broadcastRoomList();
        } catch (Exception e) {
            log.error("채팅방 입장 실패:", e);
        }
    }

    // 메시지 전송
    @MessageMapping("/chat.send")
    public void sendMessage(@Payload SendMessageRequest request,
                            SimpMessageHeaderAccessor headerAccessor) {
        try {
            log.info("========== 메시지 수신 ==========");
            log.info("Request: {}", request);

            String category = request.getCategory();
            Long userId = (Long) headerAccessor.getSessionAttributes().get("userId");

            if (userId == null) {
                throw new IllegalArgumentException("인증되지 않은 사용자");
            }

            log.info("[메시지 전송] 사용자 ID: {}, 내용: {}", userId, request.getContent());

            // User 조회
            User user = userRepository.findByIdx(userId)
                    .orElseThrow(() -> {
                        log.error("사용자 없음: {}", userId);
                        return new IllegalArgumentException("존재하지 않는 사용자");
                    });

            log.info("User 조회 성공: {}", user.getUsername());

            // 채팅방 조회
            ChatRoom room = chatRoomRepository.findByCategory(category)
                    .orElseThrow(() -> {
                        log.error("채팅방 없음: {}", category);
                        return new IllegalArgumentException("존재하지 않는 채팅방");
                    });

            log.info("ChatRoom 조회 성공: {}", room.getTitle());

            // 메시지 저장
            ChatMessage chatMessage = ChatMessage.builder()
                    .room(room)
                    .user(user)
                    .content(request.getContent())
                    .build();

            ChatMessage savedMessage = chatMessageRepository.save(chatMessage);
            log.info("DB 저장 완료: ID={}", savedMessage.getIdx());

            // dto 변환
            ChatMessageDto dto = ChatMessageDto.from(savedMessage);

            messagingTemplate.convertAndSend("/topic/room/" + category, dto);

            log.info("DTO 변환 완료: {}", dto);

            log.info("브로드캐스트 시작: /topic/room/{}", category);
        } catch (Exception e) {
            log.error("전송 실패: ", e);
        }
    }

    private void broadcastRoomList() {
        try {
            List<ChatRoomDto> rooms = chatService.getAllRooms();

            log.info("채팅방 목록 브로드캐스트 : {} 개", rooms.size());

            messagingTemplate.convertAndSend("/topic/room", rooms);
        } catch (Exception e) {
            log.error("채팅방 목록 브로드캐스트 실패: ", e);
        }
    }

}
