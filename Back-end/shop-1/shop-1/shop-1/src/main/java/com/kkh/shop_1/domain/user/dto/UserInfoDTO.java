package com.kkh.shop_1.domain.user.dto;

import com.kkh.shop_1.domain.user.entity.User;
import lombok.*;

@Getter
@AllArgsConstructor
@Builder
public class UserInfoDTO {
    private final String nickname;
    private final String email;
    private final String userRole; // SELLER / CUSTOMER

    public static UserInfoDTO from(User user) {
        return UserInfoDTO.builder()
                .nickname(user.getNickname())
                .email(user.getEmail())
                .userRole(user.getUserRole().name())
                .build();
    }
}
