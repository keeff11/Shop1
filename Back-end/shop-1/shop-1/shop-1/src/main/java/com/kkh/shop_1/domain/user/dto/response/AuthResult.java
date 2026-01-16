package com.kkh.shop_1.domain.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.ResponseCookie;

@Getter
@AllArgsConstructor
public class AuthResult {

    private boolean registered;
    private String accessCookie;
    private String refreshCookie;
    private String signUpToken;

    public static AuthResult loginSuccess(String accessToken, String refreshToken) {
        return new AuthResult(
                true,
                ResponseCookie.from("accessToken", accessToken).httpOnly(true).path("/").build().toString(),
                ResponseCookie.from("refreshToken", refreshToken).httpOnly(true).path("/").build().toString(),
                null
        );
    }

    public static AuthResult signUpRequired(String signUpToken) {
        return new AuthResult(false, null, null, signUpToken);
    }
}

