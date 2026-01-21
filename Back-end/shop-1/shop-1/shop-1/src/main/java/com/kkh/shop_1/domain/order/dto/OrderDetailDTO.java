package com.kkh.shop_1.domain.order.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kkh.shop_1.domain.order.entity.Order;
import com.kkh.shop_1.domain.order.entity.OrderItem;
import com.kkh.shop_1.domain.user.entity.Address;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderDetailDTO {

    private Long orderId;
    private String tid;
    private String status;
    private LocalDateTime orderDate;
    private List<OrderItemDTO> items;
    private int totalPrice;
    private AddressDTO address;

    /**
     * Order entity -> DTO
     */
    public static OrderDetailDTO from(Order order) {
        return OrderDetailDTO.builder()
                .orderId(order.getId())
                .tid(order.getTid())
                .status(order.getStatus().name())
                .orderDate(order.getOrderDate())
                .totalPrice(order.getTotalAmount())
                .items(order.getOrderItems().stream()
                        .map(OrderItemDTO::from)
                        .toList())
                .address(AddressDTO.from(order.getAddress()))
                .build();
    }

    /**
     * 주문 상품 상세 정보 DTO
     */
    @Getter
    @Builder(access = AccessLevel.PRIVATE)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class OrderItemDTO {
        private Long orderItemId;
        private Long itemId;
        private String itemName;
        private int quantity;
        private int price;
        private int couponDiscount;
        private int finalPrice;
        private int totalPrice;
        // ★ [추가] 리뷰 작성 여부 필드
        @JsonProperty("isReviewWritten")
        private boolean isReviewWritten;

        public static OrderItemDTO from(OrderItem orderItem) {
            return OrderItemDTO.builder()
                    .orderItemId(orderItem.getId())
                    .itemId(orderItem.getItem().getId())
                    .itemName(orderItem.getItem().getName())
                    .quantity(orderItem.getQuantity())
                    .price(orderItem.getOriginalPrice())
                    .couponDiscount(orderItem.getCouponDiscount())
                    .finalPrice(orderItem.getFinalPrice())
                    .totalPrice(orderItem.getTotalPrice())
                    // ★ [추가] 엔티티의 boolean 상태 매핑
                    .isReviewWritten(orderItem.isReviewWritten())
                    .build();
        }
    }

    /**
     * 배송지 상세 정보 DTO
     */
    @Getter
    @Builder(access = AccessLevel.PRIVATE)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class AddressDTO {
        private String zipCode;
        private String roadAddress;
        private String detailAddress;
        private String recipientName;
        private String recipientPhone;

        public static AddressDTO from(Address address) {
            return AddressDTO.builder()
                    .zipCode(address.getZipCode())
                    .roadAddress(address.getRoadAddress())
                    .detailAddress(address.getDetailAddress())
                    .recipientName(address.getRecipientName())
                    .recipientPhone(address.getRecipientPhone())
                    .build();
        }
    }
}