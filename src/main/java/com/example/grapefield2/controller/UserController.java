package com.example.grapefield2.controller;

import com.example.grapefield2.config.JwtTokenProvider;
import com.example.grapefield2.entity.User;
import com.example.grapefield2.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Tag(name = "사용자", description = "사용자 정보 관리 API")
public class UserController {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Operation(summary = "마이페이지", description = "사용자 정보 조회")
    @GetMapping("/mypage")
    public void getMyPage(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
	    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
		log.warn("마이페이지 조회: Authorization 헤더 없음");
		return;
	    }

            // jwt 토큰에서 이메일 추출
            String token = authHeader.substring(7);
            String email = jwtTokenProvider.getEmail(token);

            log.info("마이페이지 조회 요청: email={}", email);
        } catch (Exception e) {
            log.error("마이페이지 조회 중 오류", e);
        }
    }

    @Operation(summary = "정보 수정", description = "사용자 정보 업데이트")
    @PostMapping("/update")
    public ResponseEntity<?> updateUser(@RequestParam Map<String, String> request) {
        String username = request.get("username");
        String email = request.get("email");

        return ResponseEntity.ok(200);
    }
}
