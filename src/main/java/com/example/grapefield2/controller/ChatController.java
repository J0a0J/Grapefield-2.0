package com.example.grapefield2.controller;

import com.example.grapefield2.dto.ChatMessageDto;
import com.example.grapefield2.dto.ChatRoomDto;
import com.example.grapefield2.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/chat")
@Tag(name = "채팅", description = "실시간 채팅 관련 API")
public class ChatController {

    private final ChatService chatService;

    @Operation(summary = "채팅방 목록", description = "전체 채팅방 목록 조회")
    @GetMapping("/rooms")
    public ResponseEntity<List<ChatRoomDto>> getRooms() {
        List<ChatRoomDto> rooms = chatService.getAllRooms();
        return ResponseEntity.ok(rooms);
    }

    @Operation(summary = "채팅방 조회", description = "특정 채팅방 정보")
    @GetMapping("/rooms/{category}")
    public ResponseEntity<ChatRoomDto> getRoom(@PathVariable String category) {
        ChatRoomDto room = chatService.getRoomByCategory(category);
        return ResponseEntity.ok(room);
    }

    @Operation(summary = "메시지 히스토리", description = "채팅방 메시지 목록 (페이징)")
    @GetMapping("/rooms/{category}/messages")
    public ResponseEntity<List<ChatMessageDto>> getMessages(
            @PathVariable String category,
            @RequestParam(defaultValue = "0") int page) {
        Page<ChatMessageDto> messages = chatService.getMessages(category, page);
        log.debug("메시지 로드: category={}, size={}", category, messages.getContent().size());
        return ResponseEntity.ok(messages.getContent());
    }

    @Operation(summary = "접속자 수", description = "채팅방 실시간 접속자 수 (Redis 기반)")
    @GetMapping("/{category}/count")
    public ResponseEntity<Map<String, Long>> getUserCount(@PathVariable String category) {
        long count = chatService.getUserCount(category);
        return ResponseEntity.ok(Map.of("userCount", count));
    }

    @Operation(summary = "내 채팅방", description = "사용자 참여 채팅방 목록")
    @GetMapping("/rooms/my-rooms")
    public ResponseEntity<List<ChatRoomDto>> getMyRooms() {
        // 현재는 전체 목록 반환 (추후 사용자별 필터링 추가 가능)
        List<ChatRoomDto> rooms = chatService.getAllRooms();
        return ResponseEntity.ok(rooms);
    }

    @Operation(summary = "마이페이지 채팅방", description = "마이페이지용 채팅방 목록 (페이징)")
    @GetMapping("/rooms/my-page")
    public ResponseEntity<List<ChatRoomDto>> getMyPageRooms(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        // 현재는 전체 목록 반환 (추후 사용자별 필터링 추가 가능)
        List<ChatRoomDto> rooms = chatService.getAllRooms();
        return ResponseEntity.ok(rooms);
    }
    @Operation(summary = "메시지 수", description = "채팅방 총 메시지 개수")
    @GetMapping("/rooms/{category}/total")
    public ResponseEntity<Map<String, Long>> getMessageCount(@PathVariable String category) {
        long count = chatService.getMessageCount(category);
        return ResponseEntity.ok(Map.of("totalMessages", count));
    }
}
