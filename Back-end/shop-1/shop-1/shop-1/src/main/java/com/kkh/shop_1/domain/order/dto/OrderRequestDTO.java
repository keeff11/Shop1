package com.kkh.shop_1.domain.order.dto;

import com.kkh.shop_1.domain.order.entity.PaymentType;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderRequestDTO {

    private PaymentType paymentType;

    // ===== 아이템 정보 =====
    private List<ItemOrder> itemOrders;
    private String approvalUrl;
    private String cancelUrl;
    private String failUrl;
    private Integer totalAmount;
    // ===== 배송지 정보 =====
    private Long addressId;
    private String zipCode;
    private String roadAddress;
    private String detailAddress;
    private String recipientName;
    private String recipientPhone;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ItemOrder {
        private Long itemId;
        private int quantity;
        private Long couponId;
    }
}
