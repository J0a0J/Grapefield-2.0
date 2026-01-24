package com.example.grapefield2.service.oauth;

import com.example.grapefield2.enums.LoginType;
import com.example.grapefield2.service.oauth.dto.OAuthUserInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
@RequiredArgsConstructor
public class KakaoOAuthService implements OAuthService {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${kakao.client-id}")
    private String clientId;

    @Value("${kakao.redirect-uri}")
    private String redirectUri;

    @Value("${kakao.client-secret}")
    private String clientSecret;

    @Override
    public String getAccessToken(String code) {
        try {
            String tokenUrl = "https://kauth.kakao.com/oauth/token";

            String requestBody = String.format(
                    "grant_type=authorization_code&client_id=%s&redirect_uri=%s&code=%s&client_secret=%s",
                    clientId, redirectUri, code, clientSecret
            );

            System.out.println("=== 카카오 토큰 요청 ===");
            System.out.println("clientId: " + clientId);
            System.out.println("redirectUri: " + redirectUri);
            System.out.println("code: " + code);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(tokenUrl))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("=== 카카오 토큰 응답 ===");
            System.out.println("Status: " + response.statusCode());
            System.out.println("Body: " + response.body());

            JsonNode jsonNode = objectMapper.readTree(response.body());

            // 에러 체크
            if (jsonNode.get("access_token") == null) {
                throw new RuntimeException("토큰 없음: " + response.body());
            }

            return jsonNode.get("access_token").asText();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("카카오 토큰 발급 실패", e);
        }
    }

    @Override
    public OAuthUserInfo getUserInfo(String accessToken) {
        try {
            String userInfoUrl = "https://kapi.kakao.com/v2/user/me";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(userInfoUrl))
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("=== 카카오 사용자 정보 응답 ===");
            System.out.println("Body: " + response.body());

            JsonNode jsonNode = objectMapper.readTree(response.body());
            JsonNode kakaoAccount = jsonNode.get("kakao_account");
            JsonNode profile = kakaoAccount.get("profile");

            String profileImage = profile.has("profile_image_url") && !profile.get("profile_image_url").isNull()
                    ? profile.get("profile_image_url").asText()
                    : null;


            return OAuthUserInfo.builder()
                    .providerId(jsonNode.get("id").asText())
                    .email(kakaoAccount.get("email").asText())
                    .nickname(kakaoAccount.get("profile").get("nickname").asText())
                    .profileImage(profileImage)
                    .loginType(LoginType.KAKAO)
                    .build();

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("카카오 사용자 정보 조회 실패", e);
        }
    }
}