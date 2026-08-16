package com.kkh.shop_1.security.config;

/**
 *
 * 인증 없이 접근 가능한 경로 목록. SecurityConfig의 인가 설정과
 * JwtAuthenticationFilter의 "선택적 인증" 판단이 이 목록을 공유한다.
 *
 */
public final class SecurityWhitelist {

    public static final String[] PATHS = {
            "/auth/**",
            "/items/**",
            "/reviews/items/**",
            "/error",
            "/favicon.ico"
    };

    private SecurityWhitelist() {
    }
}
