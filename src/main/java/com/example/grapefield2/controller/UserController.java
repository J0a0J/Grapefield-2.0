package com.example.grapefield2.controller;

import com.example.grapefield2.config.JwtTokenProvider;
import com.example.grapefield2.entity.User;
import com.example.grapefield2.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @GetMapping("/mypage")
    public void getMyPage(@RequestHeader("Authorization") String authHeader) {
        try {
            // jwt 토큰에서 이메일 추출
            String token = authHeader.substring(7);
            String email = jwtTokenProvider.getEmail(token);

            log.info("마이페이지 조회 요청: email={}", email);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @PostMapping("/update")
    public ResponseEntity<?> updateUser(@RequestParam Map<String, String> request) {
        String username = request.get("username");
        String email = request.get("email");

        return ResponseEntity.ok(200);
    }
}
