package com.kkh.shop_1.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.kkh.shop_1.security.config.SecurityWhitelist;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String uri = request.getRequestURI();

        if (uri.startsWith("/auth/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = resolveToken(request);

        if (token != null) {
            try {
                Authentication authentication = jwtProvider.getAuthentication(token);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (RuntimeException e) {
                if (isWhitelisted(uri)) {
                    // 공개 경로는 인증이 없어도 되므로, 유효하지 않은 토큰은 무시하고 비로그인 상태로 진행한다.
                    log.warn("공개 경로에서 유효하지 않은 토큰을 무시합니다: {}", e.getMessage());
                } else {
                    // 인증이 필요한 경로에서는 JwtExceptionFilter가 처리할 수 있도록 전파한다.
                    throw e;
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isWhitelisted(String uri) {
        for (String pattern : SecurityWhitelist.PATHS) {
            if (PATH_MATCHER.match(pattern, uri)) {
                return true;
            }
        }
        return false;
    }

    private String resolveToken(HttpServletRequest request) {

        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("accessToken".equals(cookie.getName())) {
                    String value = cookie.getValue();
                    try {
                        return URLDecoder.decode(value, StandardCharsets.UTF_8);
                    } catch (Exception e) {
                        return value;
                    }
                }
            }
        }
        return null;
    }
}