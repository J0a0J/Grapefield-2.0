package com.example.grapefield2.repository;

import com.example.grapefield2.entity.ChatMessage;
import com.example.grapefield2.entity.ChatRoom;
import org.hibernate.query.criteria.JpaSearchedCase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /**
     * 특정 채팅방의 메시지 페이징 조회
     * @param room 채팅방 엔티티
     * @param pageable 페이지 정보 (size=30, sort=createdAt,desc)
     * @return 페이징된 메시지 목록
     */
    Page<ChatMessage> findByRoom(ChatRoom room, Pageable pageable);

    /**
     * 특정 채팅방의 메시지 개수
     * @param room 채팅방 엔티티
     * @return 전체 메시지 개수
     */
    long countByRoom(ChatRoom room);
}
