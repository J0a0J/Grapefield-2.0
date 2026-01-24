package com.example.grapefield2.service.oauth;

import com.example.grapefield2.service.oauth.dto.OAuthUserInfo;

public interface OAuthService {

    /**
     * Authorization code로 access token 받기
     * @param code 인증코드
     * @return access token
     */
    String getAccessToken(String code);

    /**
     * Access token 으로 사용자 정보 조회
     * @param accessToken OAuth access token
     * @return 사용자 정보
     */
    OAuthUserInfo getUserInfo(String accessToken);
}
