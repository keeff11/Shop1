package com.kkh.shop_1.domain.order.entity;

import com.kkh.shop_1.domain.coupon.entity.Coupon;
import com.kkh.shop_1.domain.item.entity.Item;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_item_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private int originalPrice;   // 주문 시점 상품 가격

    @Column(nullable = false)
    private int couponDiscount;  // 적용된 쿠폰 할인 금액

    @Column(nullable = false)
    private int finalPrice;      // 최종 결제 단가

    @Column(nullable = false)
    private boolean reviewWritten = false;

    /**
     *
     * 주문 상품 생성
     *
     */
    public static OrderItem create(Item item, int quantity) {
        if (quantity < 1) {
            throw new IllegalArgumentException("주문 수량은 1개 이상이어야 합니다.");
        }

        OrderItem orderItem = new OrderItem();
        orderItem.item = item;
        orderItem.quantity = quantity;

        int basePrice = (item.getDiscountPrice() != null) ? item.getDiscountPrice() : item.getPrice();
        orderItem.originalPrice = basePrice;
        orderItem.couponDiscount = 0;
        orderItem.finalPrice = basePrice;

        return orderItem;
    }

    /**
     *
     * 연관관계 편의 메서드: 주문 할당
     *
     */
    public void assignOrder(Order order) {
        this.order = order;
    }

    /**
     *
     * 쿠폰 적용 및 가격 재계산
     *
     */
    public void applyCoupon(Coupon coupon) {
        if (coupon == null) return;

        int discount = coupon.calculateDiscount(this.originalPrice);
        this.couponDiscount = discount;
        this.finalPrice = Math.max(0, this.originalPrice - discount); // 가격은 0원 미만 불가
    }

    /**
     *
     * 주문 취소
     *
     */
    public void cancel() {
    }

    /**
     *
     * 이 상품 항목의 총 결제 금액 계산
     *
     */
    public int getTotalPrice() {
        return getFinalPrice() * getQuantity();
    }

    /**
     *
     * 리뷰 작성 상태로 변경
     *
     */
    public void changeReviewStatus() {
        this.reviewWritten = true;
    }
}