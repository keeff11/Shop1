package com.kkh.shop_1.domain.order.dto;

import com.kkh.shop_1.domain.order.entity.Order;
import com.kkh.shop_1.domain.order.entity.OrderItem;
import com.kkh.shop_1.domain.order.entity.PaymentType;
import com.kkh.shop_1.domain.user.entity.User;
import lombok.*;

import java.util.List;

@Getter
@Builder(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentReadyRequestDTO {

    private PaymentType paymentType;
    private String partnerOrderId;
    private String partnerUserId;
    private String itemName;
    private int quantity;
    private int totalAmount;

    private String approvalUrl;
    private String cancelUrl;
    private String failUrl;

    public static PaymentReadyRequestDTO of(User user, Order order, OrderRequestDTO requestDTO, String approvalUrl) {
        return PaymentReadyRequestDTO.builder()
                .paymentType(requestDTO.getPaymentType())
                .partnerOrderId(order.getId().toString())
                .partnerUserId(user.getId().toString())
                .itemName(generateItemName(order))
                .quantity(order.getTotalQuantity())
                .totalAmount(order.getTotalAmount())
                .approvalUrl(approvalUrl)
                .cancelUrl(requestDTO.getCancelUrl())
                .failUrl(requestDTO.getFailUrl())
                .build();
    }

    /**
     *
     * (Private) 결제창에 표시될 상품명 생성
     *
     */
    private static String generateItemName(Order order) {
        List<OrderItem> items = order.getOrderItems();
        if (items.isEmpty()) {
            return "주문 상품";
        }

        String firstItemName = items.get(0).getItem().getName();
        int otherCount = items.size() - 1;

        return otherCount > 0
                ? String.format("%s 외 %d건", firstItemName, otherCount)
                : firstItemName;
    }
}