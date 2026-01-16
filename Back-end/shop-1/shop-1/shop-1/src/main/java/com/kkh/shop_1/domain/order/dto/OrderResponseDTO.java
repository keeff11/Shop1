package com.kkh.shop_1.domain.order.dto;

import com.kkh.shop_1.domain.order.entity.Order;
import com.kkh.shop_1.domain.order.entity.PaymentType;
import com.kkh.shop_1.domain.user.entity.Address;
import lombok.*;

import java.time.LocalDateTime;


@Getter
@Builder(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderResponseDTO {

    private Long orderId;
    private String tid;
    private String redirectUrl;
    private LocalDateTime orderDate;
    private PaymentType paymentType;

    // ===== 배송지 정보 =====
    private Long addressId;
    private String zipCode;
    private String roadAddress;
    private String detailAddress;
    private String recipientName;
    private String recipientPhone;

    public static OrderResponseDTO of(Order order, Address address, PaymentReadyResponseDTO paymentResponseDTO) {
        return OrderResponseDTO.builder()
                .orderId(order.getId())
                .tid(paymentResponseDTO.getTid())
                .redirectUrl(paymentResponseDTO.getRedirectUrl())
                .orderDate(order.getOrderDate())
                // .paymentType(order.getPaymentType()) // 필요 시 Order 엔티티에 결제 수단 필드 추가 후 매핑
                .addressId(address.getId())
                .zipCode(address.getZipCode())
                .roadAddress(address.getRoadAddress())
                .detailAddress(address.getDetailAddress())
                .recipientName(address.getRecipientName())
                .recipientPhone(address.getRecipientPhone())
                .build();
    }
}