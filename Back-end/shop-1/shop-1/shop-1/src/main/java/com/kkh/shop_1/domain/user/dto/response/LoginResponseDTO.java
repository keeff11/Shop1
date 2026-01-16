package com.kkh.shop_1.domain.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponseDTO {
    private final String accessToken;
    private final String refreshToken;
    private final String userRole;
}

