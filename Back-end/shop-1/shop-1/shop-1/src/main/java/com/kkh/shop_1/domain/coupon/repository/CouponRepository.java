package com.kkh.shop_1.domain.coupon.repository;

import com.kkh.shop_1.domain.coupon.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponRepository extends JpaRepository<Coupon, Long> {
}
