package com.example.grapefield2.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatSessionService {

    private final StringRedisTemplate redisTemplate;
    private static final String ROOM_USER_KEY = "chat:room:%s:users";

    public void addSession(String category, String sessionId) {
        String key = String.format(ROOM_USER_KEY, category);
        redisTemplate.opsForSet().add(key, sessionId);

        long count = getUserCount(category);
        log.info("[입장] {} 방 | 세션: {} | 현재 접속자: {}", category, sessionId, count);
    }

    public void removeSession(String category, String sessionId) {
        String key = String.format(ROOM_USER_KEY, category);
        redisTemplate.opsForSet().remove(key, sessionId);

        long count = getUserCount(category);
        log.info("[퇴장] {} 방 | 세션: {} | 현재 접속자: {}", category, sessionId, count);
    }

    // 현재 접속자 수 조회
    public long getUserCount(String category) {
        String key = String.format(ROOM_USER_KEY, category);
        Long size = redisTemplate.opsForSet().size(key);

        return size != null ? size : 0L;
    }

    // 특정 채팅방의  모든 세션 ID 조회
    public Set<String> getSessions(String category) {
        String key = String.format(ROOM_USER_KEY, category);
        return redisTemplate.opsForSet().members(key);
    }

    public boolean isSessionInRoom(String category, String sessionId) {
        String key = String.format(ROOM_USER_KEY, category);
        Boolean isMemeber = redisTemplate.opsForSet().isMember(key, sessionId);
        return isMemeber != null && isMemeber;
    }
}
