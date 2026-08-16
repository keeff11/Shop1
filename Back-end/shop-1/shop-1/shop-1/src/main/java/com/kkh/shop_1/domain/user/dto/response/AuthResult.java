package com.kkh.shop_1.domain.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthResult {

    private boolean registered;
    private String accessToken;
    private String refreshToken;
    private String signUpToken;

    public static AuthResult loginSuccess(String accessToken, String refreshToken) {
        return new AuthResult(true, accessToken, refreshToken, null);
    }

    public static AuthResult signUpRequired(String signUpToken) {
        return new AuthResult(false, null, null, signUpToken);
    }
}
