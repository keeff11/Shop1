package com.kkh.shop_1.domain.coupon.service;

import com.kkh.shop_1.domain.coupon.dto.CouponCreateRequest;
import com.kkh.shop_1.domain.coupon.dto.CouponResponse;
import com.kkh.shop_1.domain.coupon.entity.Coupon;
import com.kkh.shop_1.domain.coupon.entity.CouponType;
import com.kkh.shop_1.domain.coupon.entity.UserCoupon;
import com.kkh.shop_1.domain.coupon.repository.CouponRepository;
import com.kkh.shop_1.domain.coupon.repository.UserCouponRepository;
import com.kkh.shop_1.domain.item.entity.Item;
import com.kkh.shop_1.domain.item.service.ItemService;
import com.kkh.shop_1.domain.user.entity.User;
import com.kkh.shop_1.domain.user.entity.UserRole;
import com.kkh.shop_1.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CouponService {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
    private final UserService userService;
    private final ItemService itemService;

    /**
     *
     * 쿠폰 생성
     *
     */
    public void createCoupon(Long userId, CouponCreateRequest req) {
        User user = userService.findById(userId);

        if (req.getCouponType() == CouponType.ALL || req.getCouponType() == CouponType.CATEGORY) {
            if (user.getUserRole() != UserRole.ADMIN) {
                throw new IllegalArgumentException("관리자만 생성할 수 있는 쿠폰입니다.");
            }
        }

        Item targetItem = null;

        if (req.getCouponType() == CouponType.TARGET) {
            if (user.getUserRole() != UserRole.SELLER) {
                throw new IllegalArgumentException("판매자만 타겟 쿠폰을 생성할 수 있습니다.");
            }

            targetItem = itemService.findById(req.getItemId())
                    .orElseThrow(() -> new IllegalArgumentException("상품이 존재하지 않습니다."));

            if (!targetItem.getSeller().getId().equals(userId)) {
                throw new IllegalArgumentException("본인 상품에만 쿠폰을 생성할 수 있습니다.");
            }
        }

        Coupon coupon = Coupon.builder()
                .name(req.getName())
                .discountType(req.getDiscountType())
                .discountValue(req.getDiscountValue())
                .couponType(req.getCouponType())
                .category(req.getCategory())
                .targetItem(targetItem)
                .createdBy(user) // 위에서 찾은 user
                .expiredAt(req.getExpiredAt())
                .totalQuantity(req.getTotalQuantity())
                .issuedQuantity(0)
                .build();

        couponRepository.save(coupon);

    }

    /**
     *
     * 내 쿠폰 조회
     *
     */
    @Transactional(readOnly = true)
    public List<CouponResponse> getMyCoupons(Long userId) {
        return userCouponRepository.findByUser_Id(userId)
                .stream()
                .map(uc -> new CouponResponse(
                        uc.getCoupon().getId(),
                        uc.getCoupon().getName(),
                        uc.getCoupon().getDiscountType(),
                        uc.getCoupon().getDiscountValue(),
                        uc.getCoupon().getCouponType(),
                        uc.getCoupon().getCategory(),
                        uc.getCoupon().getTargetItem() != null
                                ? uc.getCoupon().getTargetItem().getId()
                                : null,
                        uc.isUsed()
                ))
                .toList();
    }

    /**
     *
     * 단일 쿠폰 조회
     *
     */
    @Transactional(readOnly = true)
    public Coupon getCouponById(Long couponId) {
        return couponRepository.findById(couponId)
                .orElseThrow(() -> new RuntimeException("쿠폰을 찾을 수 없습니다. ID: " + couponId));
    }

    /**
     * 🚀 [포트폴리오 핵심 로직] 선착순 쿠폰 발급 (비관적 락 적용)
     */
    public void issueCoupon(Long userId, Long couponId) {
        // 1. 비관적 락을 걸고 쿠폰 조회 (다른 스레드는 대기함)
        Coupon coupon = couponRepository.findByIdWithPessimisticLock(couponId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 쿠폰입니다."));

        // 2. 이미 발급받은 쿠폰인지 확인 (중복 발급 방지)
        if (userCouponRepository.existsByUser_IdAndCoupon_Id(userId, couponId)) {
            throw new IllegalStateException("이미 발급받은 쿠폰입니다.");
        }

        // 3. 발급 처리 (수량 검증 및 증가)
        coupon.issue();

        // 4. 유저 쿠폰 매핑 정보 저장
        User user = userService.findById(userId);
        UserCoupon userCoupon = UserCoupon.builder()
                .user(user)
                .coupon(coupon)
                .used(false)
                .build();

        userCouponRepository.save(userCoupon);
    }


}