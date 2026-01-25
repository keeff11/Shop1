package com.kkh.shop_1.domain.user.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AddressCreateRequestDTO {
    private String recipientName;
    private String recipientPhone;
    private String zipCode;
    private String roadAddress;
    private String detailAddress;

    public AddressCreateRequestDTO(String recipientName, String recipientPhone, String zipCode, String roadAddress, String detailAddress) {
        this.recipientName = recipientName;
        this.recipientPhone = recipientPhone;
        this.zipCode = zipCode;
        this.roadAddress = roadAddress;
        this.detailAddress = detailAddress;
    }
}