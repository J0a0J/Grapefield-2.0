package com.example.grapefield2.controller;

import com.example.grapefield2.dto.ChatMessageDto;
import com.example.grapefield2.dto.ChatRoomDto;
import com.example.grapefield2.service.ChatService;
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
@CrossOrigin(origins = "*")
public class ChatController {

    private final ChatService chatService;

    // 전체 채팅방 목록 조회
    @GetMapping("/rooms")
    public ResponseEntity<List<ChatRoomDto>> getRooms() {
        List<ChatRoomDto> rooms = chatService.getAllRooms();
        return ResponseEntity.ok(rooms);
    }

    // 특정 채팅방 정보 조회
    @GetMapping("/rooms/{category}")
    public ResponseEntity<ChatRoomDto> getRoom(@PathVariable String category) {
        ChatRoomDto room = chatService.getRoomByCategory(category);
        return ResponseEntity.ok(room);
    }

    @GetMapping("/rooms/my-rooms")
    public ResponseEntity<List<ChatRoomDto>> getMyRooms() {
        // 현재는 전체 목록 반환 (추후 사용자별 필터링 추가 가능)
        List<ChatRoomDto> rooms = chatService.getAllRooms();
        return ResponseEntity.ok(rooms);
    }

    // 마이페이지용 채팅방 목록 (페이징)
    @GetMapping("/rooms/my-page")
    public ResponseEntity<List<ChatRoomDto>> getMyPageRooms(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        // 현재는 전체 목록 반환 (추후 사용자별 필터링 추가 가능)
        List<ChatRoomDto> rooms = chatService.getAllRooms();
        return ResponseEntity.ok(rooms);
    }

    // 채팅방 메시지 히스토리 조회
    @GetMapping("/rooms/{category}/messages")
    public ResponseEntity<List<ChatMessageDto>> getMessages(
            @PathVariable String category,
            @RequestParam(defaultValue = "0") int page) {
        Page<ChatMessageDto> messages = chatService.getMessages(category, page);
        System.out.println("메시지 로드 됨!!!!!!!!\n\n" + messages.getContent());
        return ResponseEntity.ok(messages.getContent());
    }

    // 채팅방 접속자 수 조회
    @GetMapping("/{category}/count")
    public ResponseEntity<Map<String, Long>> getUserCount(@PathVariable String category) {
        long count = chatService.getUserCount(category);
        return ResponseEntity.ok(Map.of("userCount", count));
    }

    // 채팅방 총 메시지 개수
    @GetMapping("/rooms/{category}/total")
    public ResponseEntity<Map<String, Long>> getMessageCount(@PathVariable String category) {
        long count = chatService.getMessageCount(category);
        return ResponseEntity.ok(Map.of("totalMessages", count));
    }
}
