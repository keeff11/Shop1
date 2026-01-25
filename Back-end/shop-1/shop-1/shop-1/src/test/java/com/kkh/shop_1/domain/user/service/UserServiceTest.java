package com.kkh.shop_1.domain.user.service;

import com.kkh.shop_1.domain.user.dto.request.UserInfoUpdateRequestDTO;
import com.kkh.shop_1.domain.user.dto.request.UserPasswordUpdateRequestDTO;
import com.kkh.shop_1.domain.user.dto.response.UserInfoResponseDTO;
import com.kkh.shop_1.domain.user.dto.response.UserInfoUpdateResponseDTO;
import com.kkh.shop_1.domain.user.entity.User;
import com.kkh.shop_1.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("사용자 조회 성공")
    void findById_Success() {
        // given
        Long userId = 1L;
        User user = mock(User.class);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        User result = userService.findById(userId);

        // then
        assertThat(result).isEqualTo(user);
    }

    @Test
    @DisplayName("사용자 조회 실패 - 존재하지 않음")
    void findById_Fail() {
        // given
        Long userId = 999L;
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.findById(userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("사용자를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("내 정보 상세 조회 성공")
    void findUserInfo_Success() {
        // given
        Long userId = 1L;
        User user = mock(User.class);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(user.getNickname()).willReturn("nickname");
        given(user.getEmail()).willReturn("email@test.com");
        // UserInfoResponseDTO.from 내부 구현에 따라 추가 mocking 필요할 수 있음

        // when
        UserInfoResponseDTO result = userService.findUserInfo(userId);

        // then
        assertThat(result.getNickname()).isEqualTo("nickname");
    }

    @Test
    @DisplayName("프로필 수정 성공")
    void updateUserProfile_Success() {
        // given
        Long userId = 1L;
        UserInfoUpdateRequestDTO dto = new UserInfoUpdateRequestDTO("newNick");
        User user = mock(User.class);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(user.getNickname()).willReturn("newNick"); // updateProfile 호출 후 상태 가정

        // when
        UserInfoUpdateResponseDTO result = userService.updateUserProfile(userId, dto);

        // then
        verify(user).updateProfile("newNick", null);
        assertThat(result.getNickname()).isEqualTo("newNick");
    }

    @Test
    @DisplayName("비밀번호 변경 성공")
    void updateUserPassword_Success() {
        // given
        Long userId = 1L;
        UserPasswordUpdateRequestDTO dto = new UserPasswordUpdateRequestDTO("oldPass", "newPass");
        User user = mock(User.class);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(user.getPassword()).willReturn("encodedOldPass");
        given(passwordEncoder.matches("oldPass", "encodedOldPass")).willReturn(true);
        given(passwordEncoder.encode("newPass")).willReturn("encodedNewPass");

        // when
        userService.updateUserPassword(userId, dto);

        // then
        verify(user).changePassword("encodedNewPass");
    }

    @Test
    @DisplayName("비밀번호 변경 실패 - 현재 비밀번호 불일치")
    void updateUserPassword_Fail_WrongPassword() {
        // given
        Long userId = 1L;
        UserPasswordUpdateRequestDTO dto = new UserPasswordUpdateRequestDTO("wrongPass", "newPass");
        User user = mock(User.class);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(user.getPassword()).willReturn("encodedOldPass");
        given(passwordEncoder.matches("wrongPass", "encodedOldPass")).willReturn(false);

        // when & then
        assertThatThrownBy(() -> userService.updateUserPassword(userId, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("현재 비밀번호가 일치하지 않습니다.");
    }
}
