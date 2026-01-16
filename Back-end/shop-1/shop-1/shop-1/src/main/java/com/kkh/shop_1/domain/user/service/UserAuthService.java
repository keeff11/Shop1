package com.kkh.shop_1.domain.user.service;

import com.kkh.shop_1.domain.user.dto.*;
import com.kkh.shop_1.domain.user.dto.request.LocalLoginRequestDTO;
import com.kkh.shop_1.domain.user.dto.request.LocalSignUpRequestDTO;
import com.kkh.shop_1.domain.user.dto.request.SocialLoginRequestDTO;
import com.kkh.shop_1.domain.user.dto.response.AuthResult;
import com.kkh.shop_1.domain.user.dto.response.LocalSignUpResponseDTO;
import com.kkh.shop_1.domain.user.dto.response.LoginResponseDTO;
import com.kkh.shop_1.domain.user.entity.Address;
import com.kkh.shop_1.domain.user.entity.LoginType;
import com.kkh.shop_1.domain.user.entity.User;
import com.kkh.shop_1.domain.user.repository.UserRepository;
import com.kkh.shop_1.security.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * 사용자 인증 및 회원가입 전반을 관리하는 서비스
 *
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserAuthService {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;
    private final KakaoService kakaoService;
    private final NaverService naverService;

    /**
     *
     * 로컬 계정 회원가입 처리
     *
     */
    @Transactional
    public LocalSignUpResponseDTO localSignUp(LocalSignUpRequestDTO dto) {
        validateDuplicate(dto.getEmail(), dto.getNickname());

        User user = User.createLocalUser(
                dto.getEmail(),
                passwordEncoder.encode(dto.getPassword()),
                dto.getNickname(),
                dto.getUserRole()
        );

        registerUserWithAddress(user, dto);

        LoginResponseDTO loginResponse = createLoginResponse(user);
        return new LocalSignUpResponseDTO(
                loginResponse.getAccessToken(),
                loginResponse.getRefreshToken(),
                user.getUserRole().name()
        );
    }

    /**
     *
     * 로컬 이메일 로그인
     *
     */
    public LoginResponseDTO localLogin(LocalLoginRequestDTO dto) {
        User user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        return createLoginResponse(user);
    }

    /**
     *
     * 카카오 로그인 프로세스 (인가 코드 기반)
     *
     */
    public AuthResult processKakaoLogin(String code) {
        KakaoUserInfoDTO userInfo = kakaoService.getKakaoUserInfo(code);
        return handleSocialLogin(LoginType.KAKAO, String.valueOf(userInfo.getSocialId()), userInfo);
    }

    /**
     *
     * 네이버 로그인 프로세스 (인가 코드 및 상태값 기반)
     *
     */
    public AuthResult processNaverLogin(String code, String state) {
        NaverUserInfoDTO userInfo = naverService.getNaverUserInfo(code, state);
        return handleSocialLogin(LoginType.NAVER, userInfo.getSocialId(), userInfo);
    }

    /**
     *
     * 통합 소셜 회원가입 완료 로직
     *
     */
    @Transactional
    public LoginResponseDTO completeSocialSignUp(String provider, String signUpToken, SocialLoginRequestDTO dto) {
        LoginType type = LoginType.valueOf(provider.toUpperCase());
        User user;

        if (type == LoginType.KAKAO) {
            KakaoUserInfoDTO info = jwtProvider.parseKakaoSignUpToken(signUpToken);
            user = User.createSocialUser(type, String.valueOf(info.getSocialId()), info.getEmail(), dto.getNickname(), info.getProfileImage(), dto.getUserRole());
        } else {
            NaverUserInfoDTO info = jwtProvider.parseNaverSignUpToken(signUpToken);
            user = User.createSocialUser(type, info.getSocialId(), info.getEmail(), dto.getNickname(), info.getProfileImage(), dto.getUserRole());
        }

        registerUserWithAddress(user, dto);
        return createLoginResponse(user);
    }

    /**
     *
     * 중복 체크 로직들
     *
     */
    public boolean checkEmailDuplicate(String email) { return userRepository.existsByEmail(email); }
    public boolean checkNicknameDuplicate(String nickname) { return userRepository.existsByNickname(nickname); }

    // --- Private Helper Methods ---

    /**
     *
     * 소셜 로그인 공통 판단 (가입 여부에 따라 토큰 혹은 가입 권유 반환)
     *
     */
    private AuthResult handleSocialLogin(LoginType type, String socialId, Object userInfo) {
        return userRepository.findByLoginTypeAndSocialId(type, socialId)
                .map(user -> createAuthResult(user))
                .orElseGet(() -> {
                    String token = (type == LoginType.KAKAO)
                            ? jwtProvider.createKakaoSignUpToken((KakaoUserInfoDTO) userInfo)
                            : jwtProvider.createNaverSignUpToken((NaverUserInfoDTO) userInfo);
                    return AuthResult.signUpRequired(token);
                });
    }

    /**
     *
     * 유저 저장 및 초기 배송지 연동 공통 로직
     *
     */
    private void registerUserWithAddress(User user, Object dto) {
        user.addAddress(createInitialAddress(dto, user));
        userRepository.save(user);
    }

    private Address createInitialAddress(Object dto, User user) {
        if (dto instanceof LocalSignUpRequestDTO d) {
            return Address.createDefault(user, d.getZipCode(), d.getRoadAddress(), d.getDetailAddress(), d.getRecipientName(), d.getRecipientPhone());
        } else if (dto instanceof SocialLoginRequestDTO d) {
            return Address.createDefault(user, d.getZipCode(), d.getRoadAddress(), d.getDetailAddress(), d.getRecipientName(), d.getRecipientPhone());
        }
        throw new IllegalArgumentException("지원하지 않는 주소 형식입니다.");
    }

    private LoginResponseDTO createLoginResponse(User user) {
        return new LoginResponseDTO(
                jwtProvider.createAccessToken(user.getId(), user.getUserRole().name()),
                jwtProvider.createRefreshToken(user.getId()),
                user.getUserRole().name()
        );
    }

    private AuthResult createAuthResult(User user) {
        return AuthResult.loginSuccess(
                jwtProvider.createAccessToken(user.getId(), user.getUserRole().name()),
                jwtProvider.createRefreshToken(user.getId())
        );
    }

    private void validateDuplicate(String email, String nickname) {
        if (checkEmailDuplicate(email)) throw new IllegalStateException("이미 존재하는 이메일입니다.");
        if (checkNicknameDuplicate(nickname)) throw new IllegalStateException("이미 존재하는 닉네임입니다.");
    }
}