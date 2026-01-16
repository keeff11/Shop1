package com.kkh.shop_1.domain.user.dto.request;

import lombok.*;

@Getter
@AllArgsConstructor
public class UserPasswordUpdateRequestDTO {

    private String currentPassword;
    private String newPassword;

}
