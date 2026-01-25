package com.kkh.shop_1.domain.coupon.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kkh.shop_1.domain.coupon.dto.CouponCreateRequest;
import com.kkh.shop_1.domain.coupon.dto.CouponResponse;
import com.kkh.shop_1.domain.coupon.entity.CouponType;
import com.kkh.shop_1.domain.coupon.entity.DiscountType;
import com.kkh.shop_1.domain.coupon.service.CouponService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CouponController.class)
class CouponControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CouponService couponService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("쿠폰 생성 성공")
    @WithMockUser(username = "1", roles = "ADMIN")
    void createCoupon_Success() throws Exception {
        // given
        CouponCreateRequest request = new CouponCreateRequest("name", DiscountType.FIXED, 1000, CouponType.ALL, null, null, LocalDateTime.now());

        // when & then
        mockMvc.perform(post("/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        verify(couponService).createCoupon(eq(1L), any(CouponCreateRequest.class));
    }

    @Test
    @DisplayName("내 쿠폰 조회 성공")
    @WithMockUser(username = "1", roles = "USER")
    void getMyCoupons_Success() throws Exception {
        // given
        CouponResponse response = new CouponResponse(1L, "name", DiscountType.FIXED, 1000, CouponType.ALL, null, null, false);
        List<CouponResponse> responses = Collections.singletonList(response);

        given(couponService.getMyCoupons(1L)).willReturn(responses);

        // when & then
        mockMvc.perform(get("/coupons/my")
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].couponId").value(1L));
    }
}
