package com.kkh.shop_1.domain.user.dto.response;

import com.kkh.shop_1.domain.user.entity.Address;
import lombok.*;

@Getter
@Builder(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AddressResponseDTO {

    private Long id;
    private String zipCode;
    private String roadAddress;
    private String detailAddress;
    private String recipientName;
    private String recipientPhone;
    private boolean isDefault;

    /**
     *
     * Address 엔티티를 응답 DTO로 변환
     *
     */
    public static AddressResponseDTO from(Address address) {
        return AddressResponseDTO.builder()
                .id(address.getId())
                .zipCode(address.getZipCode())
                .roadAddress(address.getRoadAddress())
                .detailAddress(address.getDetailAddress())
                .recipientName(address.getRecipientName())
                .recipientPhone(address.getRecipientPhone())
                .isDefault(address.isDefault())
                .build();
    }
}