package com.kkh.shop_1.domain.user.service;

import com.kkh.shop_1.common.s3.S3Service;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final S3Service s3Service; // [추가] S3 업로드 서비스 주입

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. ID: " + id));
    }

    public UserInfoResponseDTO findUserInfo(Long userId) {
        User user = findById(userId);
        return UserInfoResponseDTO.from(user);
    }

    /**
     * [수정] 프로필 수정 (이미지 파일 처리 추가)
     */
    @Transactional
    public UserInfoUpdateResponseDTO updateUserProfile(Long userId, UserInfoUpdateRequestDTO dto, MultipartFile profileImg) {
        User user = findById(userId);

        String profileImgUrl = user.getProfileImg(); // 기존 이미지 URL

        // 1. 새 이미지가 업로드된 경우 S3 업로드
        if (profileImg != null && !profileImg.isEmpty()) {
            try {
                // 기존 이미지가 있고 S3 URL이라면 삭제 로직 추가 가능 (선택사항)

                // 새 이미지 업로드 (경로: users/{userId})
                profileImgUrl = s3Service.uploadImage("users/" + userId, profileImg);
            } catch (IOException e) {
                throw new RuntimeException("프로필 이미지 업로드 실패", e);
            }
        }

        // 2. 엔티티 업데이트 (닉네임, 이미지URL)
        // User 엔티티에 updateProfile(String nickname, String profileImg) 메서드가 있어야 함
        user.updateProfile(dto.getNickname(), profileImgUrl);

        return UserInfoUpdateResponseDTO.from(user);
    }

    // [유지] 기존 메서드 (호환성 위해 남겨둘 경우)
    @Transactional
    public UserInfoUpdateResponseDTO updateUserProfile(Long userId, UserInfoUpdateRequestDTO dto) {
        return updateUserProfile(userId, dto, null);
    }

    @Transactional
    public void updateUserPassword(Long userId, UserPasswordUpdateRequestDTO dto) {
        User user = findById(userId);
        validatePassword(dto.getCurrentPassword(), user.getPassword());
        String encodedPassword = passwordEncoder.encode(dto.getNewPassword());
        user.changePassword(encodedPassword);
    }

    private void validatePassword(String rawPassword, String encodedPassword) {
        if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }
    }
}