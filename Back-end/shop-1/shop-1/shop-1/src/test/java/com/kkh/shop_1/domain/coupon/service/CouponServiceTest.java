package com.kkh.shop_1.domain.coupon.service;

import com.kkh.shop_1.domain.coupon.dto.CouponCreateRequest;
import com.kkh.shop_1.domain.coupon.dto.CouponResponse;
import com.kkh.shop_1.domain.coupon.entity.Coupon;
import com.kkh.shop_1.domain.coupon.entity.CouponType;
import com.kkh.shop_1.domain.coupon.entity.DiscountType;
import com.kkh.shop_1.domain.coupon.entity.UserCoupon;
import com.kkh.shop_1.domain.coupon.repository.CouponRepository;
import com.kkh.shop_1.domain.coupon.repository.UserCouponRepository;
import com.kkh.shop_1.domain.item.entity.Item;
import com.kkh.shop_1.domain.item.service.ItemService;
import com.kkh.shop_1.domain.user.entity.User;
import com.kkh.shop_1.domain.user.entity.UserRole;
import com.kkh.shop_1.domain.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @InjectMocks
    private CouponService couponService;

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private UserCouponRepository userCouponRepository;

    @Mock
    private UserService userService;

    @Mock
    private ItemService itemService;

    @Test
    @DisplayName("관리자 쿠폰 생성 성공")
    void createCoupon_Admin_Success() {
        // given
        Long userId = 1L;
        CouponCreateRequest req = new CouponCreateRequest("name", DiscountType.FIXED, 1000, CouponType.ALL, null, null, LocalDateTime.now());
        User admin = mock(User.class);

        given(userService.findById(userId)).willReturn(admin);
        given(admin.getUserRole()).willReturn(UserRole.ADMIN);

        // when
        couponService.createCoupon(userId, req);

        // then
        verify(couponRepository).save(any(Coupon.class));
    }

    @Test
    @DisplayName("관리자 쿠폰 생성 실패 - 권한 없음")
    void createCoupon_Admin_Fail_NoPermission() {
        // given
        Long userId = 1L;
        CouponCreateRequest req = new CouponCreateRequest("name", DiscountType.FIXED, 1000, CouponType.ALL, null, null, LocalDateTime.now());
        User user = mock(User.class);

        given(userService.findById(userId)).willReturn(user);
        given(user.getUserRole()).willReturn(UserRole.CUSTOMER);

        // when & then
        assertThatThrownBy(() -> couponService.createCoupon(userId, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("관리자만 생성할 수 있는 쿠폰입니다.");
    }

    @Test
    @DisplayName("타겟 쿠폰 생성 성공")
    void createCoupon_Target_Success() {
        // given
        Long userId = 1L;
        Long itemId = 100L;
        CouponCreateRequest req = new CouponCreateRequest("name", DiscountType.FIXED, 1000, CouponType.TARGET, null, itemId, LocalDateTime.now());
        User seller = mock(User.class);
        Item item = mock(Item.class);

        given(userService.findById(userId)).willReturn(seller);
        given(seller.getUserRole()).willReturn(UserRole.SELLER);
        given(itemService.findById(itemId)).willReturn(Optional.of(item));
        given(item.getSeller()).willReturn(seller);
        given(seller.getId()).willReturn(userId);

        // when
        couponService.createCoupon(userId, req);

        // then
        verify(couponRepository).save(any(Coupon.class));
    }

    @Test
    @DisplayName("내 쿠폰 조회 성공")
    void getMyCoupons_Success() {
        // given
        Long userId = 1L;
        UserCoupon userCoupon = mock(UserCoupon.class);
        Coupon coupon = mock(Coupon.class);

        given(userCouponRepository.findByUser_Id(userId)).willReturn(List.of(userCoupon));
        given(userCoupon.getCoupon()).willReturn(coupon);
        given(coupon.getId()).willReturn(10L);
        given(coupon.getName()).willReturn("coupon");

        // when
        List<CouponResponse> result = couponService.getMyCoupons(userId);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("coupon");
    }
}
