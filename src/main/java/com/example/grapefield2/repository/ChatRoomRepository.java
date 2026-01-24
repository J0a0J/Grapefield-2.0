package com.example.grapefield2.repository;

import com.example.grapefield2.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    // 카테고리로 채팅방 찾기
    Optional<ChatRoom> findByCategory(String category);

    // 카테고리 존재 여부
    boolean existsByCategory(String category);
}
