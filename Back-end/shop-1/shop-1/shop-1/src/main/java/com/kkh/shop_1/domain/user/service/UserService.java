package com.kkh.shop_1.domain.user.service;

import com.kkh.shop_1.domain.user.dto.request.UserInfoUpdateRequestDTO;
import com.kkh.shop_1.domain.user.dto.request.UserPasswordUpdateRequestDTO;
import com.kkh.shop_1.domain.user.dto.response.UserInfoResponseDTO;
import com.kkh.shop_1.domain.user.dto.response.UserInfoUpdateResponseDTO;
import com.kkh.shop_1.domain.user.entity.User;
import com.kkh.shop_1.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * 사용자 기본 정보 관리 서비스
 *
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     *
     * ID로 사용자 엔티티 조회
     *
     */
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. ID: " + id));
    }

    /**
     *
     * 내 정보 상세 조회
     *
     */
    public UserInfoResponseDTO findUserInfo(Long userId) {
        User user = findById(userId);
        return UserInfoResponseDTO.from(user);
    }

    /**
     *
     * 프로필 정보(닉네임 등) 수정
     *
     */
    @Transactional
    public UserInfoUpdateResponseDTO updateUserProfile(Long userId, UserInfoUpdateRequestDTO dto) {
        User user = findById(userId);

        user.updateProfile(dto.getNickname(), null); // profileImg 필드 추가 대응 가능

        return UserInfoUpdateResponseDTO.from(user);
    }

    /**
     *
     * 비밀번호 변경 (로컬 로그인 사용자 전용)
     *
     */
    @Transactional
    public void updateUserPassword(Long userId, UserPasswordUpdateRequestDTO dto) {
        User user = findById(userId);

        validatePassword(dto.getCurrentPassword(), user.getPassword());

        String encodedPassword = passwordEncoder.encode(dto.getNewPassword());
        user.changePassword(encodedPassword);
    }

    // --- Private Helper Methods ---

    /**
     *
     * 비밀번호 일치 여부 검증
     *
     */
    private void validatePassword(String rawPassword, String encodedPassword) {
        if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }
    }



}