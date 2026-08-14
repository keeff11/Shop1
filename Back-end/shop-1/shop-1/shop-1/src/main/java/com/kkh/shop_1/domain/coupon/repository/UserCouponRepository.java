package com.kkh.shop_1.domain.coupon.repository;

import com.kkh.shop_1.domain.coupon.entity.UserCoupon;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserCouponRepository extends JpaRepository<UserCoupon, Long> {

    List<UserCoupon> findByUser_Id(Long userId);
    boolean existsByUser_IdAndCoupon_Id(Long userId, Long couponId);
    Optional<UserCoupon> findByUser_IdAndCoupon_Id(Long userId, Long couponId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT uc FROM UserCoupon uc WHERE uc.id = :id")
    Optional<UserCoupon> findByIdWithPessimisticLock(@Param("id") Long id);
}