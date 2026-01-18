package com.kkh.shop_1.domain.user.controller;

import com.kkh.shop_1.common.ApiResponse;
import com.kkh.shop_1.domain.user.dto.*;
import com.kkh.shop_1.domain.user.dto.request.LocalLoginRequestDTO;
import com.kkh.shop_1.domain.user.dto.request.LocalSignUpRequestDTO;
import com.kkh.shop_1.domain.user.dto.request.SocialLoginRequestDTO;
import com.kkh.shop_1.domain.user.dto.response.AuthResult;
import com.kkh.shop_1.domain.user.dto.response.LocalSignUpResponseDTO;
import com.kkh.shop_1.domain.user.dto.response.LoginResponseDTO;
import com.kkh.shop_1.domain.user.entity.User;
import com.kkh.shop_1.domain.user.service.*;
import com.kkh.shop_1.security.jwt.JwtProvider;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class UserAuthController {

    private final UserService userService;
    private final UserAuthService userAuthService;
    private final JwtProvider jwtProvider;
    private final EmailService emailService;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    private static final String ACCESS_TOKEN = "accessToken";
    private static final String REFRESH_TOKEN = "refreshToken";

    /**
     *
     * 세션 확인
     *
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserInfoDTO>> getMyInfo(
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String headerToken,
            @CookieValue(name = ACCESS_TOKEN, required = false) String cookieToken
    ) {
        String accessToken = resolveToken(headerToken, cookieToken);

        if (accessToken == null || !jwtProvider.validateToken(accessToken)) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.<UserInfoDTO>fail("로그인 필요"));
        }

        Long userId = Long.valueOf(jwtProvider.parseClaims(accessToken).getSubject());
        User user = userService.findById(userId);

        return ResponseEntity.ok(ApiResponse.success(UserInfoDTO.from(user)));
    }

    /**
     *
     * 로컬 이메일 회원가입 (웹/앱 공통)
     *
     */
    @PostMapping("/local/sign-up")
    public ResponseEntity<ApiResponse<LocalSignUpResponseDTO>> signUp(
            @RequestBody LocalSignUpRequestDTO dto,
            HttpServletResponse response
    ) {
        LocalSignUpResponseDTO result = userAuthService.localSignUp(dto);
        handleTokenResponse(response, result.getAccessToken(), result.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     *
     * 이메일 로그인
     *
     */
    @PostMapping("/local/login")
    public ResponseEntity<ApiResponse<LoginResponseDTO>> localLogin(
            @RequestBody LocalLoginRequestDTO dto,
            HttpServletResponse response
    ) {
        LoginResponseDTO result = userAuthService.localLogin(dto);
        handleTokenResponse(response, result.getAccessToken(), result.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     *
     * [웹 전용] 소셜 로그인 콜백 핸들러 (Redirect 처리)
     *
     */
    @GetMapping("/{provider}/callback")
    public void socialCallbackWeb(
            @PathVariable String provider,
            @RequestParam String code,
            @RequestParam(required = false) String state,
            HttpServletResponse response
    ) throws IOException {
        AuthResult result = "kakao".equals(provider)
                ? userAuthService.processKakaoLogin(code)
                : userAuthService.processNaverLogin(code, state);

        handleSocialRedirect(response, result, provider);
    }

    /**
     *
     * [앱 전용] 소셜 로그인 콜백 핸들러 (JSON 처리)
     *
     */
    @PostMapping("/{provider}/callback")
    public ResponseEntity<ApiResponse<AuthResult>> socialCallbackApp(
            @PathVariable String provider,
            @RequestBody Map<String, String> request
    ) {
        String code = request.get("code");
        String state = request.get("state");

        AuthResult result = "kakao".equals(provider)
                ? userAuthService.processKakaoLogin(code)
                : userAuthService.processNaverLogin(code, state);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     *
     * 소셜 가입 완료 및 로그인 (카카오/네이버 통합)
     *
     */
    @PostMapping("/{provider}/login")
    public ResponseEntity<ApiResponse<LoginResponseDTO>> socialSignUp(
            @PathVariable String provider,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String signUpToken,
            @RequestBody SocialLoginRequestDTO dto,
            HttpServletResponse response
    ) {
        LoginResponseDTO result = userAuthService.completeSocialSignUp(provider, signUpToken, dto);
        handleTokenResponse(response, result.getAccessToken(), result.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     *
     * 로그아웃 처리 (인증 쿠키 무효화)
     *
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletResponse response) {
        clearAuthCookies(response);
        return ResponseEntity.ok(ApiResponse.successNoData());
    }

    /**
     *
     * 이메일 중복 체크
     *
     */
    @GetMapping("/check-email")
    public ResponseEntity<ApiResponse<Boolean>> checkEmailDuplicate(@RequestParam String email) {
        return ResponseEntity.ok(ApiResponse.success(userAuthService.checkEmailDuplicate(email)));
    }

    /**
     *
     * 닉네임 중복 체크
     *
     */
    @GetMapping("/check-nickname")
    public ResponseEntity<ApiResponse<Boolean>> checkNicknameDuplicate(@RequestParam String nickname) {
        return ResponseEntity.ok(ApiResponse.success(userAuthService.checkNicknameDuplicate(nickname)));
    }

    /**
     *
     * 이메일 인증코드 발송
     *
     */

    @PostMapping("/email/send-code")
    public ResponseEntity<ApiResponse<String>> sendEmailCode(@RequestParam String email) {
        emailService.sendVerificationCode(email);
        return ResponseEntity.ok(ApiResponse.success("인증코드가 발송되었습니다."));
    }

    /**
     *
     * 이메일 인증코드 검증
     *
     */
    @PostMapping("/email/verify-code")
    public ResponseEntity<ApiResponse<Boolean>> verifyEmailCode(
            @RequestParam String email,
            @RequestParam String code
    ) {
        boolean isVerified = emailService.verifyCode(email, code);
        return ResponseEntity.ok(ApiResponse.success(isVerified));
    }

    // --- Private Helper Methods ---

    /**
     *
     * 헤더 또는 쿠키에서 토큰을 추출함
     *
     */
    private String resolveToken(String headerToken, String cookieToken) {
        if (headerToken != null && headerToken.startsWith("Bearer ")) {
            return headerToken.substring(7);
        }
        return cookieToken;
    }

    /**
     *
     * 액세스 및 리프레시 토큰을 응답 쿠키에 설정함
     *
     */
    private void handleTokenResponse(HttpServletResponse response, String access, String refresh) {
        setCookie(response, ACCESS_TOKEN, access, 30 * 60);
        setCookie(response, REFRESH_TOKEN, refresh, 7 * 24 * 60 * 60);
    }

    /**
     *
     * 모든 인증 관련 쿠키를 만료시켜 삭제함
     *
     */
    private void clearAuthCookies(HttpServletResponse response) {
        setCookie(response, ACCESS_TOKEN, "", 0);
        setCookie(response, REFRESH_TOKEN, "", 0);
    }

    /**
     *
     * 상세 쿠키 설정값 정의 및 응답 헤더 추가
     *
     */
    private void setCookie(HttpServletResponse response, String name, String value, long maxAge) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(false) // 배포 시 true
                .path("/")
                .maxAge(maxAge)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    /**
     *
     * 소셜 가입 여부에 따른 리다이렉트 경로 분기 처리
     *
     */
    private void handleSocialRedirect(HttpServletResponse response, AuthResult result, String provider) throws IOException {
        if (result.isRegistered()) {
            response.addHeader(HttpHeaders.SET_COOKIE, result.getAccessCookie());
            response.addHeader(HttpHeaders.SET_COOKIE, result.getRefreshCookie());
            response.sendRedirect(frontendUrl);
        } else {
            response.sendRedirect(String.format("%s/register/social/additional-info?provider=%s&%sToken=%s",
                    frontendUrl, provider, provider, result.getSignUpToken()));
        }
    }
}