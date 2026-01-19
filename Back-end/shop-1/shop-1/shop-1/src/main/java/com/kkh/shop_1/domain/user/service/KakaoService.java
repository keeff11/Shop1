package com.kkh.shop_1.domain.user.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kkh.shop_1.domain.user.dto.KakaoUserInfoDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 *
 * 카카오 소셜 로그인 연동 및 사용자 정보 획득 서비스
 *
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoService {

    @Value("${kakao.apikey}")
    private String kakaoApiKey;

    @Value("${kakao.redirect-uri}")
    private String kakaoRedirectUri;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     *
     * [통합 메서드] 인가 코드를 받아 카카오 사용자 정보까지 한 번에 획득
     *
     */
    public KakaoUserInfoDTO getKakaoUserInfo(String code) {
        String accessToken = getKakaoToken(code);
        return getKakaoUserInfoByToken(accessToken);
    }

    /**
     *
     * Authorization Code를 이용해 카카오로부터 Access Token을 발급받음
     *
     */
    public String getKakaoToken(String code) {
        String url = "https://kauth.kakao.com/oauth/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", kakaoApiKey);
        params.add("redirect_uri", kakaoRedirectUri); // 프론트엔드 REDIRECT_URI와 반드시 일치해야 함
        params.add("code", code);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        log.info("### 카카오 토큰 요청 시도 ###");
        log.info("보내는 Redirect URI: [{}]", kakaoRedirectUri);
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
            JsonNode json = objectMapper.readTree(response.getBody());
            return json.get("access_token").asText();
        } catch (HttpClientErrorException e) {
            log.error("Kakao Token Request Failed: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("카카오 토큰 발급 실패: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new RuntimeException("카카오 토큰 파싱 중 오류 발생", e);
        }
    }

    /**
     *
     * Access Token을 이용해 카카오 사용자 프로필 정보를 조회함
     *
     */
    public KakaoUserInfoDTO getKakaoUserInfoByToken(String accessToken) {
        String url = "https://kapi.kakao.com/v2/user/me";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
            JsonNode json = objectMapper.readTree(response.getBody());

            Long socialId = json.get("id").asLong();
            JsonNode account = json.get("kakao_account");
            JsonNode profile = account.get("profile");

            return new KakaoUserInfoDTO(
                    socialId,
                    account.has("email") ? account.get("email").asText() : null,
                    profile.has("nickname") ? profile.get("nickname").asText() : null,
                    profile.has("profile_image_url") ? profile.get("profile_image_url").asText() : null
            );
        } catch (HttpClientErrorException e) {
            log.error("Kakao User Info Request Failed: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("카카오 유저 정보 조회 실패");
        } catch (Exception e) {
            throw new RuntimeException("카카오 유저 정보 파싱 실패", e);
        }
    }
}