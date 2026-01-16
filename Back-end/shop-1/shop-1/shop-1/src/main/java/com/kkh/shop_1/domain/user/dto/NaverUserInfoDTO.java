package com.kkh.shop_1.domain.user.dto;

import lombok.*;

@Getter
@AllArgsConstructor
public class NaverUserInfoDTO {

    private String socialId;
    private String email;
    private String name;
    private String profileImage;

}

