package com.example.grapefield2.dto;

import com.example.grapefield2.entity.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageDto {

    private Long idx;
    private Long roomId;
    private String category;
    // 채팅 보낸 사람 정보
    private Long userId;
    private String nickname;

    private String content;
    private LocalDateTime createdAt;

    public static ChatMessageDto from(ChatMessage message) {
        return ChatMessageDto.builder()
                .idx(message.getIdx())
                .roomId(message.getRoom().getIdx())
                .category(message.getRoom().getCategory())
                .userId(message.getUser().getIdx())
                .nickname(message.getUser().getUsername())
                .content(message.getContent())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
