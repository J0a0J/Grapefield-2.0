package com.example.grapefield2.controller;

import com.example.grapefield2.config.JwtTokenProvider;
import com.example.grapefield2.entity.EmailVerify;
import com.example.grapefield2.entity.User;
import com.example.grapefield2.enums.LoginType;
import com.example.grapefield2.repository.EmailVerifyRepository;
import com.example.grapefield2.repository.UserRepository;
import com.example.grapefield2.service.EmailService;
import com.example.grapefield2.service.UserService;
import com.example.grapefield2.service.oauth.KakaoOAuthService;
import com.example.grapefield2.service.oauth.dto.OAuthUserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {

    private final UserService userService;
    private final KakaoOAuthService kakaoOAuthService;
    private final EmailService emailService;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final EmailVerifyRepository emailVerifyRepository;

    /**
     * 일반 회원가입
     * @param request
     * @return
     */
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String password = request.get("password");
            String username = request.get("username");
            String verifyCode = request.get("verifyCode");

            EmailVerify emailVerify = emailVerifyRepository
                    .findByEmailAndVerifyCode(email, verifyCode)
                    .orElse(null);

            if (emailVerify == null || !emailVerify.isVerified()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "이메일 인증을 완료해주세요"));
            }

            User user = User.builder()
                    .email(email)
                    .password(passwordEncoder.encode(password))
                    .username(username)
                    .loginType(LoginType.NORMAL)
                    .isEmailVerified(true) // 이미 인증됨
                    .profileImg(userService.createDefaultProfileImage(username))
                    .build();

            userRepository.save(user);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "회원가입 성공!"
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 회원가입 전 이메일 인증번호 발송
     */
    @PostMapping("/send-verification")
    public ResponseEntity<?> sendVerification(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");

            // 이메일 중복 확인
            if (userRepository.existsByEmail(email)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "이미 사용 중인 이메일입니다."
                ));
            }

            // 기존 미인증 코드 삭제
            emailVerifyRepository.findByEmailAndVerifyCode(email, null)
                    .ifPresent(emailVerifyRepository::delete);

            String verifyCode = String.format("%06d", (int) (Math.random() * 1000000));

            EmailVerify emailVerify = EmailVerify.builder()
                    .email(email)
                    .verifyCode(verifyCode)
                    .expiresAt(LocalDateTime.now().plusMinutes(10))
                    .isVerified(false)
                    .build();

            emailVerifyRepository.save(emailVerify);

            // 이메일 발송
            emailService.sendVerificationCode(email, verifyCode);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "인증번호가 발송되었습니다."
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "이메일 발송 실패: " + e.getMessage()
            ));
        }
    }

    /**
     * 이메일 인증번호 확인
     */
    @PostMapping("/verify-code")
    public ResponseEntity<?> verifyCode(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String code = request.get("code").trim();

            boolean success = emailService.verifyCode(email, code);

            if (success) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "이메일 인증 완료"
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "인증번호가 올바르지 않거나 만료되었습니다."
                ));
            }
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/password-verify")
    public ResponseEntity<?> verifyPassword(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String password = request.get("password");

            User user = userRepository.findByEmail(email).orElse(null);

            if (user == null) {
                log.warn("사용자를 찾을 수 없음: email={}", email);
                return ResponseEntity.ok(false);
            }

            boolean isMatch = passwordEncoder.matches(password, user.getPassword());

            log.info("비밀번호 확인 결과: {}", isMatch);

            return ResponseEntity.ok(isMatch);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(false);
        }
    }

    /**
     * 카카오 로그인
     * @param request
     * @return
     */
    @PostMapping("/kakao")
    public ResponseEntity<?> kakaoLogin(@RequestBody Map<String, String> request) {
        try {
            String code = request.get("code");

            System.out.println("=== 카카오 로그인 시작 ===");
            System.out.println("받은 code: " + code);

            String accessToken = kakaoOAuthService.getAccessToken(code);
            System.out.println("받은 accessToken: " + accessToken);

            OAuthUserInfo oauthUserInfo = kakaoOAuthService.getUserInfo(accessToken);
            System.out.println("받은 사용자 정보: " + oauthUserInfo.getEmail());

            User user = userService.oauthLogin(oauthUserInfo);
            String token = jwtTokenProvider.createToken(user.getEmail());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "카카오 로그인 성공",
                    "token", token,
                    "user", Map.of(
                            "email", user.getEmail(),
                            "username", user.getUsername(),
                            "profileImg", user.getProfileImg()
                    )
            ));
        } catch (Exception e) {
            System.err.println("=== 카카오 로그인 실패 ===");
            e.printStackTrace();

            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "카카오 로그인 실패 : " + e.getMessage()
            ));
        }
    }

    /**
     * 이메일 중복 체크
     * @param email
     * @return
     */
    @GetMapping("/check-email")
    public ResponseEntity<?> checkEmail(@RequestParam String email) {
        boolean exists = userService.checkEmailDuplicate(email);

        return ResponseEntity.ok(Map.of(
                "exists", exists,
                "message", exists ? "이미 사용 중인 이메일입니다." : "사용 가능한 이메일입니다."
        ));
    }

    /**
     * 닉네임 중복 체크
     * @param username
     * @return
     */
    @GetMapping("/check-username")
    public ResponseEntity<?> checkUsername(@RequestParam String username) {
        boolean exists = userService.checkUsernameDuplicate(username);

        return ResponseEntity.ok(Map.of(
                "exists", exists,
                "message", exists ? "이미 사용 중인 닉네임입니다." : "시용 가능한 닉네임입니다"
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String password = request.get("password");

            // 사용자 조회
            User user = userService.findByEmail(email);

            if (user == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "이메일 또는 비밀번호가 올바르지 않습니다."
                ));
            }

            // 비밀번호 확인
            if (!passwordEncoder.matches(password, user.getPassword())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "이메일 또는 비밀번호가 올바르지 않습니다."
                ));
            }

            // 이메일 인증 확인
            if (!user.isEmailVerified()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "이메일 인증을 완료해주세요."
                ));
            }

            // JWT 토큰 발급
            String token = jwtTokenProvider.createToken(user.getEmail());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "로그인 성공",
                    "token", token,
                    "user", Map.of(
                            "userIdx", user.getIdx(),
                            "email", user.getEmail(),
                            "username", user.getUsername(),
                            "profileImg", user.getProfileImg()
                    )
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "로그인 실패: " +e.getMessage()
            ));
        }
    }
}
