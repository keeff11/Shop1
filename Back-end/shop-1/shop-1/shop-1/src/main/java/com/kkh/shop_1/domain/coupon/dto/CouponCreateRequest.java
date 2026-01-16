package com.kkh.shop_1.domain.coupon.dto;

import com.kkh.shop_1.domain.coupon.entity.CouponType;
import com.kkh.shop_1.domain.coupon.entity.DiscountType;
import com.kkh.shop_1.domain.item.entity.ItemCategory;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class CouponCreateRequest {

    private String name;

    private DiscountType discountType;
    private int discountValue;

    private CouponType couponType;

    private ItemCategory category; // CATEGORY
    private Long itemId;           // TARGET

    private LocalDateTime expiredAt;
}


