package com.kkh.shop_1.domain.user.dto.request;

import com.kkh.shop_1.domain.user.entity.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SocialLoginRequestDTO {

    // 인가 코드
    private String code;

    public UserRole userRole;
    public String nickname;

    // ===== 주소 =====
    public String zipCode;
    public String roadAddress;
    public String detailAddress;
    public String recipientName;
    public String recipientPhone;
}


