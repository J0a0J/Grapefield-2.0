package com.example.grapefield2.service;

import com.example.grapefield2.dto.ChatMessageDto;
import com.example.grapefield2.dto.ChatRoomDto;
import com.example.grapefield2.entity.ChatRoom;
import com.example.grapefield2.repository.ChatMessageRepository;
import com.example.grapefield2.repository.ChatRoomRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatSessionService chatSessionService;

    private static final int PAGE_SIZE = 30;

    // 접속자 수를 포함한 전체 채팅방 목록 조회
    public List<ChatRoomDto> getAllRooms() {
        List<ChatRoom> rooms = chatRoomRepository.findAll();

        return rooms.stream()
                .map(room -> {
                    long userCount = chatSessionService.getUserCount(room.getCategory());
                    return ChatRoomDto.from(room, userCount);
                })
                .collect(Collectors.toList());
    }

    // 카테고리로 채팅방 조회
    public ChatRoomDto getRoomByCategory(String category) {
        ChatRoom room = chatRoomRepository.findByCategory(category)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 채팅방: " + category));

        long userCount = chatSessionService.getUserCount(category);
        return ChatRoomDto.from(room, userCount);
    }

    // 채팅방 메시지 히스토리 조회
    public Page<ChatMessageDto> getMessages(String category, int page) {
        ChatRoom room = chatRoomRepository.findByCategory(category)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 채팅방: " + category));

        Pageable pageable = PageRequest.of(page, PAGE_SIZE, Sort.by("createdAt").descending());

        Page<ChatMessageDto> messages = chatMessageRepository.findByRoom(room, pageable)
                .map(ChatMessageDto::from);

        log.info("채팅방 {} 메시지 조회: page={}, total={}", category, page, messages.getTotalElements());

        return messages;
    }

    // 특정 채팅방의 현재 메시지 개수
    public long getMessageCount(String category) {
        ChatRoom room = chatRoomRepository.findByCategory(category)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 채팅방 : " + category));

        return chatMessageRepository.countByRoom(room);
    }

    // 특정 채팅방의 현재 접속자 수
    public long getUserCount(String category) {
        long count = chatSessionService.getUserCount(category);
        log.info("{} 채팅방 인원: {}", category, count);
        return count;
    }
}
