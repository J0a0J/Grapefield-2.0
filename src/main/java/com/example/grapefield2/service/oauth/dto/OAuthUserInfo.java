package com.example.grapefield2.service.oauth.dto;

import com.example.grapefield2.enums.LoginType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OAuthUserInfo {
    private String providerId;
    private String email;
    private String nickname;
    private String profileImage;
    private LoginType loginType; // KAKAO, NAVER, GOOGLE 등
}
