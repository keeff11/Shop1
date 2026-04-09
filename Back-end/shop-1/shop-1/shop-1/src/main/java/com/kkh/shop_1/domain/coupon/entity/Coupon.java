package com.kkh.shop_1.domain.coupon.entity;

import com.kkh.shop_1.domain.item.entity.Item;
import com.kkh.shop_1.domain.item.entity.ItemCategory;
import com.kkh.shop_1.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Enumerated(EnumType.STRING)
    private DiscountType discountType;

    private int discountValue;

    @Enumerated(EnumType.STRING)
    private CouponType couponType;

    @Enumerated(EnumType.STRING)
    private ItemCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private Item targetItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    private LocalDateTime expiredAt;

    // --- 추가된 필드 ---
    private int totalQuantity;
    private int issuedQuantity;

    // 할인 금액 계산
    public int calculateDiscount(int originalPrice) {
        if (discountType == DiscountType.FIXED) {
            return Math.min(discountValue, originalPrice);
        }
        return originalPrice * discountValue / 100;
    }

    // 쿠폰 발급
    public void issue() {
        if (this.issuedQuantity >= this.totalQuantity) {
            throw new IllegalStateException("준비된 쿠폰이 모두 소진되었습니다.");
        }
        if (this.expiredAt.isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("만료된 쿠폰입니다.");
        }
        this.issuedQuantity++;
    }
}