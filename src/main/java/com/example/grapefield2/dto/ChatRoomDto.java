package com.example.grapefield2.dto;

import com.example.grapefield2.entity.ChatMessage;
import com.example.grapefield2.entity.ChatRoom;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoomDto {

    private Long idx;
    private String category;
    private String title;
    private Long userCount;
    private LocalDateTime createdAt;

    // entity -> dto 변환 (userCount 제외)
    public static ChatRoomDto from(ChatRoom chatRoom) {
        return ChatRoomDto.builder()
                .idx(chatRoom.getIdx())
                .category(chatRoom.getCategory())
                .title(chatRoom.getTitle())
                .createdAt(chatRoom.getCreatedAt())
                .build();
    }

    // entity -> dto 변환 (userCount 포함 - redis에서 가져올 예정)
    public static ChatRoomDto from(ChatRoom chatRoom, Long userCount) {
        return ChatRoomDto.builder()
                .idx(chatRoom.getIdx())
                .category(chatRoom.getCategory())
                .title(chatRoom.getTitle())
                .createdAt(chatRoom.getCreatedAt())
                .userCount(userCount)
                .build();
    }
}
