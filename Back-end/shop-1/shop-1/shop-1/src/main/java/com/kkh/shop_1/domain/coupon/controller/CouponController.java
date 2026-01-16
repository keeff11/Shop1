package com.kkh.shop_1.domain.coupon.controller;

import com.kkh.shop_1.common.ApiResponse;
import com.kkh.shop_1.domain.coupon.dto.CouponCreateRequest;
import com.kkh.shop_1.domain.coupon.dto.CouponResponse;
import com.kkh.shop_1.domain.coupon.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/coupons")
public class CouponController {

    private final CouponService couponService;

    /**
     *
     * 쿠폰 생성
     *
     */
    @PostMapping
    public ApiResponse<?> createCoupon(@AuthenticationPrincipal Long userId, @RequestBody CouponCreateRequest request) throws IOException {
        couponService.createCoupon(userId, request);
        return ApiResponse.successNoData();
    }

    /**
     *
     * 내 쿠폰 조회
     *
     */
    @GetMapping("/my")
    public ApiResponse<List<CouponResponse>> getMyCoupons(@AuthenticationPrincipal Long userId) {
        return ApiResponse.success(couponService.getMyCoupons(userId));
    }
}


