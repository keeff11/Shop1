package com.kkh.shop_1.domain.order.dto;

import com.kkh.shop_1.domain.order.entity.Order;
import com.kkh.shop_1.domain.order.entity.PaymentType;
import com.kkh.shop_1.domain.user.entity.Address;
import lombok.*;

import java.time.LocalDateTime;


@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderResponseDTO {

    private Long orderId;
    private String tid;
    private String redirectUrl;
    private LocalDateTime orderDate;
    private PaymentType paymentType;
    // 서버가 계산한 실제 결제 금액. PG 결제창(토스 SDK 등)에는 클라이언트 계산값이 아니라 이 값을 넘긴다.
    private Integer totalAmount;

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
                .paymentType(order.getPaymentType())
                .totalAmount(order.getTotalAmount())
                .addressId(address.getId())
                .zipCode(address.getZipCode())
                .roadAddress(address.getRoadAddress())
                .detailAddress(address.getDetailAddress())
                .recipientName(address.getRecipientName())
                .recipientPhone(address.getRecipientPhone())
                .build();
    }
}