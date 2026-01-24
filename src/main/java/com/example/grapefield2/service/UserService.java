package com.example.grapefield2.service;

import com.example.grapefield2.entity.User;
import com.example.grapefield2.enums.LoginType;
import com.example.grapefield2.repository.UserRepository;
import com.example.grapefield2.service.oauth.dto.OAuthUserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
//    private final EmailVerifyRepository emailVerifyRepository;

    /**
     * OAuth 로그인 (카카오 등)
     * @param oAuthUserInfo
     * @return
     */
    @Transactional
    public User oauthLogin(OAuthUserInfo oAuthUserInfo) {
        // providerId로 기존 회원 확인
        return userRepository.findByProviderId(oAuthUserInfo.getProviderId())
                .orElseGet(() -> {
                    // 신규 회원 생성
                    User newUser = User.builder()
                            .email(oAuthUserInfo.getEmail())
                            .password(null)
                            .username(oAuthUserInfo.getNickname())
                            .loginType(oAuthUserInfo.getLoginType())
                            .providerId(oAuthUserInfo.getProviderId())
                            .profileImg(oAuthUserInfo.getProfileImage())
                            .isEmailVerified(true) // OAuth는 이메일 인증 불필요
                            .build();

                    return userRepository.save(newUser);
                });
    }

    /**
     * 이메일로 사용자 조회
     */
    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    /**
     * 이메일 중복 확인
     * @param email
     * @return
     */
    public boolean checkEmailDuplicate(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * 닉네임 중복 확인
     * @param username
     * @return
     */
    public boolean checkUsernameDuplicate(String username) {
        return userRepository.existsByUsername(username);
    }

    /**
     * DiceBear API로 기본 프로필 이미지 url 생성
     * @param username
     * @return
     */
    public String createDefaultProfileImage(String username) {
        return "https://api.dicebear.com/7.x/initials/svg?seed=" + username;
    }

}
