package com.kkh.shop_1.domain.user.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kkh.shop_1.domain.user.dto.NaverUserInfoDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 *
 * 네이버 소셜 로그인 연동 서비스
 *
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NaverService {

    @Value("${naver.client-id}")
    private String clientId;

    @Value("${naver.client-secret}")
    private String clientSecret;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     *
     * [통합 메서드] 인가 코드와 상태값으로 유저 정보까지 한 번에 가져오기
     *
     */
    public NaverUserInfoDTO getNaverUserInfo(String code, String state) {
        String accessToken = getNaverAccessToken(code, state);
        return getNaverUserInfo(accessToken);
    }

    /**
     *
     * Authorization Code → Access Token
     *
     */
    public String getNaverAccessToken(String code, String state) {
        String url = "https://nid.naver.com/oauth2.0/token";

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("grant_type", "authorization_code")
                .queryParam("client_id", clientId)
                .queryParam("client_secret", clientSecret)
                .queryParam("code", code)
                .queryParam("state", state);

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(builder.toUriString(), String.class);
            JsonNode json = objectMapper.readTree(response.getBody());
            return json.get("access_token").asText();
        } catch (Exception e) {
            log.error("Naver Token Request Failed: {}", e.getMessage());
            throw new RuntimeException("네이버 액세스 토큰 획득 실패", e);
        }
    }

    /**
     *
     * Access Token → User Info
     *
     */
    public NaverUserInfoDTO getNaverUserInfo(String accessToken) {
        String url = "https://openapi.naver.com/v1/nid/me";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            JsonNode responseNode = objectMapper.readTree(response.getBody()).get("response");

            return new NaverUserInfoDTO(
                    responseNode.get("id").asText(),
                    responseNode.has("email") ? responseNode.get("email").asText() : null,
                    responseNode.has("name") ? responseNode.get("name").asText() : null,
                    responseNode.has("profile_image") ? responseNode.get("profile_image").asText() : null
            );
        } catch (Exception e) {
            log.error("Naver User Info Request Failed: {}", e.getMessage());
            throw new RuntimeException("네이버 유저 정보 파싱 실패", e);
        }
    }
}