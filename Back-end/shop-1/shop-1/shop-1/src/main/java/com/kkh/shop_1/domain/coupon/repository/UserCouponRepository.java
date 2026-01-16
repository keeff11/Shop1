package com.kkh.shop_1.domain.coupon.repository;

import com.kkh.shop_1.domain.coupon.entity.UserCoupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserCouponRepository extends JpaRepository<UserCoupon, Long> {

    List<UserCoupon> findByUser_Id(Long userId);
}


