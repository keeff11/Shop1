package com.kkh.shop_1.domain.coupon.service;

import com.kkh.shop_1.domain.coupon.dto.CouponCreateRequest;
import com.kkh.shop_1.domain.coupon.dto.CouponResponse;
import com.kkh.shop_1.domain.coupon.entity.Coupon;
import com.kkh.shop_1.domain.coupon.entity.CouponType;
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
                .createdBy(user)
                .expiredAt(req.getExpiredAt())
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


}