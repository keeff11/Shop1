package com.kkh.shop_1.security.jwt;

import com.kkh.shop_1.domain.user.dto.KakaoUserInfoDTO;
import com.kkh.shop_1.domain.user.dto.NaverUserInfoDTO;
import com.kkh.shop_1.domain.user.entity.LoginType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtProvider {


    @Value("${jwt.secret.key}")
    private String secretKey;

    // =========================
    // Token Expiration
    // =========================
    private static final long ACCESS_TOKEN_EXPIRATION = 1000L * 60 * 300; // 300분
    private static final long REFRESH_TOKEN_EXPIRATION = 1000L * 60 * 60 * 24 * 7; // 7일
    private static final long SIGN_UP_TOKEN_EXPIRATION = 1000L * 60 * 100; // 100분

    // =========================
    // Access / Refresh Token
    // =========================
    public String createAccessToken(Long userId, String userRole) {
        return createAuthToken(userId.toString(), userRole, TokenType.ACCESS, ACCESS_TOKEN_EXPIRATION);
    }

    public String createRefreshToken(Long userId) {
        return createAuthToken(userId.toString(), null, TokenType.REFRESH, REFRESH_TOKEN_EXPIRATION);
    }

    private String createAuthToken(String subject, String userRole, TokenType tokenType, long expiration) {
        Claims claims = Jwts.claims().setSubject(subject);
        claims.put("tokenType", tokenType.name());
        if (userRole != null) claims.put("userRole", userRole);
        return buildToken(claims, expiration);
    }

    // =========================
    // SignUp Token (Social)
    // =========================

    /** Kakao SignUp Token 생성 */
    public String createKakaoSignUpToken(KakaoUserInfoDTO kakaoUserInfo) {
        return createSocialSignUpToken(
                LoginType.KAKAO,
                String.valueOf(kakaoUserInfo.getSocialId()),
                kakaoUserInfo.getEmail(),
                kakaoUserInfo.getProfileImage()
        );
    }

    /** Naver SignUp Token 생성 */
    public String createNaverSignUpToken(NaverUserInfoDTO naverUserInfo) {
        return createSocialSignUpToken(
                LoginType.NAVER,
                naverUserInfo.getSocialId(),
                naverUserInfo.getEmail(),
                naverUserInfo.getProfileImage()
        );
    }

    private String createSocialSignUpToken(LoginType loginType, String socialId, String email, String profileImage) {
        Claims claims = Jwts.claims();
        claims.put("tokenType", TokenType.SIGN_UP.name());
        claims.put("loginType", loginType.name());
        claims.put("socialId", socialId);
        claims.put("email", email);
        claims.put("profileImage", profileImage);
        return buildToken(claims, SIGN_UP_TOKEN_EXPIRATION);
    }

    // =========================
    // SignUp Token Parsing
    // =========================

    /** Kakao SignUp Token 파싱 */
    public KakaoUserInfoDTO parseKakaoSignUpToken(String token) {
        Claims claims = validateAndGetSignUpClaims(token, LoginType.KAKAO);
        return new KakaoUserInfoDTO(
                Long.valueOf(claims.get("socialId").toString()),
                (String) claims.get("email"),
                null,
                (String) claims.get("profileImage")
        );
    }

    /** Naver SignUp Token 파싱 */
    public NaverUserInfoDTO parseNaverSignUpToken(String token) {
        Claims claims = validateAndGetSignUpClaims(token, LoginType.NAVER);
        return new NaverUserInfoDTO(
                (String) claims.get("socialId"),
                (String) claims.get("email"),
                null,
                (String) claims.get("profileImage")
        );
    }

    private Claims validateAndGetSignUpClaims(String token, LoginType expectedType) {
        Claims claims = parseClaims(token);
        if (!TokenType.SIGN_UP.name().equals(claims.get("tokenType"))) {
            throw new IllegalArgumentException("Invalid SignUp Token");
        }
        if (!expectedType.name().equals(claims.get("loginType"))) {
            throw new IllegalArgumentException("Invalid LoginType for SignUp Token");
        }
        return claims;
    }

    // =========================
    // Authentication (수정됨)
    // =========================
    public Authentication getAuthentication(String token) {
        Claims claims = parseClaims(token);

        // 토큰 타입 검증
        if (!TokenType.ACCESS.name().equals(claims.get("tokenType"))) {
            throw new IllegalArgumentException("Not an access token");
        }

        // DB를 거치지 않고 토큰에서 바로 userId와 Role 추출
        Long userId = Long.valueOf(claims.getSubject());
        String userRole = (String) claims.get("userRole");

        // 권한 리스트 생성 (Role이 있으면 생성, 없으면 빈 리스트)
        Collection<? extends GrantedAuthority> authorities = (userRole != null)
                ? Collections.singletonList(new SimpleGrantedAuthority(userRole))
                : Collections.emptyList();

        // Principal 자리에 UserDetails 대신 userId(Long) 자체를 넣음
        return new UsernamePasswordAuthenticationToken(userId, null, authorities);
    }

    // =========================
    // Validation / Parsing
    // =========================
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey.getBytes())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // =========================
    // Common Builder
    // =========================
    private String buildToken(Claims claims, long expiration) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expiration);
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(Keys.hmacShaKeyFor(secretKey.getBytes()), SignatureAlgorithm.HS256)
                .compact();
    }
}
