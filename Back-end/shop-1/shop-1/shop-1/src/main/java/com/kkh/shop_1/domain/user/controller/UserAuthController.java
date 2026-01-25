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
public class UserAuthController {

    private final UserService userService;
    private final UserAuthService userAuthService;
    private final JwtProvider jwtProvider;
    private final EmailService emailService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    private static final String ACCESS_TOKEN = "accessToken";
    private static final String REFRESH_TOKEN = "refreshToken";

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserInfoDTO>> getMyInfo(
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String headerToken,
            @CookieValue(name = ACCESS_TOKEN, required = false) String cookieToken
    ) {
        String accessToken = resolveToken(headerToken, cookieToken);
        if (accessToken == null || !jwtProvider.validateToken(accessToken)) {
            return ResponseEntity.status(401).body(ApiResponse.<UserInfoDTO>fail("로그인 필요"));
        }
        Long userId = Long.valueOf(jwtProvider.parseClaims(accessToken).getSubject());
        User user = userService.findById(userId);
        return ResponseEntity.ok(ApiResponse.success(UserInfoDTO.from(user)));
    }

    @PostMapping("/local/sign-up")
    public ResponseEntity<ApiResponse<LocalSignUpResponseDTO>> signUp(
            @RequestBody LocalSignUpRequestDTO dto, HttpServletResponse response
    ) {
        LocalSignUpResponseDTO result = userAuthService.localSignUp(dto);
        handleTokenResponse(response, result.getAccessToken(), result.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/local/login")
    public ResponseEntity<ApiResponse<LoginResponseDTO>> localLogin(
            @RequestBody LocalLoginRequestDTO dto, HttpServletResponse response
    ) {
        LoginResponseDTO result = userAuthService.localLogin(dto);
        handleTokenResponse(response, result.getAccessToken(), result.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{provider}/callback")
    public void socialCallbackWeb(
            @PathVariable String provider, @RequestParam String code,
            @RequestParam(required = false) String state, HttpServletResponse response
    ) throws IOException {
        AuthResult result = "kakao".equals(provider)
                ? userAuthService.processKakaoLogin(code)
                : userAuthService.processNaverLogin(code, state);
        handleSocialRedirect(response, result, provider);
    }

    @PostMapping("/{provider}/callback")
    public ResponseEntity<ApiResponse<AuthResult>> socialCallbackApp(
            @PathVariable String provider, @RequestBody Map<String, String> request
    ) {
        String code = request.get("code");
        String state = request.get("state");
        AuthResult result = "kakao".equals(provider)
                ? userAuthService.processKakaoLogin(code)
                : userAuthService.processNaverLogin(code, state);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/{provider}/login")
    public ResponseEntity<ApiResponse<LoginResponseDTO>> socialSignUp(
            @PathVariable String provider, @RequestHeader(HttpHeaders.AUTHORIZATION) String signUpToken,
            @RequestBody SocialLoginRequestDTO dto, HttpServletResponse response
    ) {
        LoginResponseDTO result = userAuthService.completeSocialSignUp(provider, signUpToken, dto);
        handleTokenResponse(response, result.getAccessToken(), result.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletResponse response) {
        clearAuthCookies(response);
        return ResponseEntity.ok(ApiResponse.successNoData());
    }

    @GetMapping("/check-email")
    public ResponseEntity<ApiResponse<Boolean>> checkEmailDuplicate(@RequestParam String email) {
        return ResponseEntity.ok(ApiResponse.success(userAuthService.checkEmailDuplicate(email)));
    }

    // [핵심] 닉네임 중복 체크 API
    @GetMapping("/check-nickname")
    public ResponseEntity<ApiResponse<Boolean>> checkNicknameDuplicate(@RequestParam String nickname) {
        return ResponseEntity.ok(ApiResponse.success(userAuthService.checkNicknameDuplicate(nickname)));
    }

    @PostMapping("/email/send-code")
    public ResponseEntity<ApiResponse<String>> sendEmailCode(@RequestParam String email) {
        emailService.sendVerificationCode(email);
        return ResponseEntity.ok(ApiResponse.success("인증코드가 발송되었습니다."));
    }

    @PostMapping("/email/verify-code")
    public ResponseEntity<ApiResponse<Boolean>> verifyEmailCode(@RequestParam String email, @RequestParam String code) {
        boolean isVerified = emailService.verifyCode(email, code);
        return ResponseEntity.ok(ApiResponse.success(isVerified));
    }

    private String resolveToken(String headerToken, String cookieToken) {
        if (headerToken != null && headerToken.startsWith("Bearer ")) {
            return headerToken.substring(7);
        }
        return cookieToken;
    }

    private void handleTokenResponse(HttpServletResponse response, String access, String refresh) {
        setCookie(response, ACCESS_TOKEN, access, 30 * 60);
        setCookie(response, REFRESH_TOKEN, refresh, 7 * 24 * 60 * 60);
    }

    private void clearAuthCookies(HttpServletResponse response) {
        setCookie(response, ACCESS_TOKEN, "", 0);
        setCookie(response, REFRESH_TOKEN, "", 0);
    }

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