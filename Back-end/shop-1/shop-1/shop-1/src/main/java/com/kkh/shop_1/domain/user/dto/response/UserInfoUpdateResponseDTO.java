package com.kkh.shop_1.domain.user.dto.response;

import com.kkh.shop_1.domain.user.entity.User;
import lombok.*;

/**
 * * 사용자 정보 수정 결과 응답 DTO
 * */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public class UserInfoUpdateResponseDTO {

    private final String nickname;

    /**
     * * User 엔티티를 수정 결과 DTO로 변환
     * */
    public static UserInfoUpdateResponseDTO from(User user) {
        return UserInfoUpdateResponseDTO.builder()
                .nickname(user.getNickname())
                .build();
    }
}