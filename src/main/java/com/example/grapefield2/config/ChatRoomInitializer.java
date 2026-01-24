package com.example.grapefield2.config;

import com.example.grapefield2.entity.ChatRoom;
import com.example.grapefield2.repository.ChatRoomRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatRoomInitializer implements ApplicationRunner {

    private final ChatRoomRepository chatRoomRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<String> categories = Arrays.asList(
                "연극", "뮤지컬", "콘서트", "클래식", "국악", "무용"
        );

        for (String category : categories) {
            // 이미 존재하면 실행 안 함
            if (chatRoomRepository.existsByCategory(category)) {
                log.info("채팅방 이미 존재 : {}", category);
                continue;
            }

            // 새로 생성
            ChatRoom chatRoom = ChatRoom.builder()
                    .category(category)
                    .title(category + " 이야기방")
                    .build();

            chatRoomRepository.save(chatRoom);
            log.info("채팅방 생성  완료: {}", category);
        }
        log.info("총 {} 개 채팅방 준비 완료", chatRoomRepository.count());
    }
}
