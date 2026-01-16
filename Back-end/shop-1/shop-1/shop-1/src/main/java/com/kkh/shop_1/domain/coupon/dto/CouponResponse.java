package com.kkh.shop_1.domain.coupon.dto;

import com.kkh.shop_1.domain.coupon.entity.CouponType;
import com.kkh.shop_1.domain.coupon.entity.DiscountType;
import com.kkh.shop_1.domain.item.entity.ItemCategory;
import lombok.*;

@Getter
@AllArgsConstructor
public class CouponResponse {

    private Long couponId;
    private String name;

    private DiscountType discountType;
    private int discountValue;

    private CouponType couponType;

    private ItemCategory category;
    private Long itemId;

    private boolean used;
}


