package com.kkh.shop_1.domain.user.dto.request;

import com.kkh.shop_1.domain.user.entity.UserRole;
import lombok.Getter;

@Getter
public class LocalSignUpRequestDTO {

    public String email;
    public String password;
    public UserRole userRole;
    public String nickname;

    // ===== 주소 =====
    public String zipCode;
    public String roadAddress;
    public String detailAddress;
    public String recipientName;
    public String recipientPhone;
}
