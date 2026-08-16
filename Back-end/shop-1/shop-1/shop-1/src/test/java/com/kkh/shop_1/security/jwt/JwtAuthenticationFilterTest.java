package com.kkh.shop_1.security.jwt;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * * JwtAuthenticationFilter 단위 테스트
 * * 핵심 검증 대상: 토큰 검증 중 발생한 RuntimeException을
 *   화이트리스트 경로에서만 무시하고, 인증이 필요한 경로에서는 그대로 전파하는 보안 로직
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtProvider jwtProvider;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtProvider);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("토큰이 없는 요청")
    class NoToken {

        @Test
        @DisplayName("Authorization 헤더와 쿠키 모두 없으면 인증 없이 체인이 진행된다")
        void noToken_chainProceeds_authenticationRemainsNull() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/orders/1");
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            filter.doFilter(request, response, chain);

            assertThat(chain.getRequest()).isNotNull();
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }
    }

    @Nested
    @DisplayName("유효한 토큰")
    class ValidToken {

        @Test
        @DisplayName("유효한 토큰이면 SecurityContext에 인증 정보를 설정하고 체인을 진행한다")
        void validToken_setsAuthentication_chainProceeds() throws Exception {
            String token = "valid-token";
            Authentication authentication = mock(Authentication.class);
            given(jwtProvider.getAuthentication(token)).willReturn(authentication);

            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/orders/1");
            request.addHeader("Authorization", "Bearer " + token);
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            filter.doFilter(request, response, chain);

            assertThat(chain.getRequest()).isNotNull();
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isEqualTo(authentication);
        }
    }

    @Nested
    @DisplayName("유효하지 않은 토큰")
    class InvalidToken {

        @Test
        @DisplayName("화이트리스트 경로에서는 유효하지 않은 토큰 예외를 무시하고 비로그인 상태로 체인을 진행한다")
        void invalidToken_onWhitelistedPath_ignoredAndChainProceeds() throws Exception {
            String token = "bad-token";
            given(jwtProvider.getAuthentication(token)).willThrow(new RuntimeException("invalid token"));

            // SecurityWhitelist.PATHS 의 "/items/**" 패턴에 매칭되는 경로
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/items/1");
            request.addHeader("Authorization", "Bearer " + token);
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            filter.doFilter(request, response, chain);

            assertThat(chain.getRequest()).isNotNull();
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("화이트리스트에 없는 경로에서는 유효하지 않은 토큰 예외가 그대로 전파된다")
        void invalidToken_onProtectedPath_exceptionPropagates() {
            String token = "bad-token";
            RuntimeException tokenException = new RuntimeException("invalid token");
            given(jwtProvider.getAuthentication(token)).willThrow(tokenException);

            // 화이트리스트("/auth/**", "/items/**", "/reviews/items/**", "/error", "/favicon.ico")에 속하지 않는 보호 경로
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/orders/1");
            request.addHeader("Authorization", "Bearer " + token);
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            assertThatThrownBy(() -> filter.doFilter(request, response, chain))
                    .isInstanceOf(RuntimeException.class)
                    .isSameAs(tokenException);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }
    }

    @Nested
    @DisplayName("/auth/ 경로")
    class AuthPathBypass {

        @Test
        @DisplayName("/auth/ 로 시작하는 경로는 토큰 검증 자체를 건너뛰고 체인이 진행된다")
        void authPath_skipsTokenValidation_evenWithInvalidToken() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
            request.addHeader("Authorization", "Bearer garbage-token");
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            filter.doFilter(request, response, chain);

            assertThat(chain.getRequest()).isNotNull();
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verifyNoInteractions(jwtProvider);
        }
    }
}
