package com.kkh.shop_1.domain.user.dto.response;

import com.kkh.shop_1.domain.user.entity.LoginType;
import com.kkh.shop_1.domain.user.entity.User;
import com.kkh.shop_1.domain.user.entity.UserRole;
import lombok.*;

/**
 * * 사용자 상세 정보 응답 DTO
 * */
@Getter
@Builder(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserInfoResponseDTO {

    private LoginType loginType;
    private String email;
    private String profileImg;
    private String nickname;
    private UserRole userRole;

    /**
     * * User 엔티티를 응답 DTO로 변환
     * */
    public static UserInfoResponseDTO from(User user) {
        return UserInfoResponseDTO.builder()
                .loginType(user.getLoginType())
                .email(user.getEmail())
                .profileImg(user.getProfileImg())
                .nickname(user.getNickname())
                .userRole(user.getUserRole())
                .build();
    }
}