package com.kkh.shop_1.domain.order.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OrderStatus {

    PAYMENT_PENDING("결제 대기"),
    PAID("결제 완료"),
    CANCELLED("주문 취소"),
    SHIPPING("배송 중"),
    DELIVERED("배송 완료");

    private final String description;
}