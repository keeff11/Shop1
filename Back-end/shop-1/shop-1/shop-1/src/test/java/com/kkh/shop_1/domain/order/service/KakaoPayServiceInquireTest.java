package com.kkh.shop_1.domain.order.service;

import com.kkh.shop_1.domain.order.entity.Order;
import com.kkh.shop_1.domain.user.entity.Address;
import com.kkh.shop_1.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class KakaoPayServiceInquireTest {

    @InjectMocks
    private KakaoPayService kakaoPayService;

    @Mock
    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(kakaoPayService, "cid", "TC0ONETIME");
        ReflectionTestUtils.setField(kakaoPayService, "secretKey", "test_secret_dummy");
    }

    private Order buildOrder(String tid) {
        Order order = Order.create(mock(User.class), mock(Address.class));
        ReflectionTestUtils.setField(order, "tid", tid);
        return order;
    }

    // 실제 호출로 확인함: 카카오페이 주문 조회는 GET+쿼리파라미터가 아니라 POST+JSON body만 받는다
    // (approve/cancel과 동일하게 postForEntity 사용)

    @Test
    @DisplayName("status가 SUCCESS_PAYMENT면 PAID로 판단한다")
    void inquire_SuccessPayment_ReturnsPaid() {
        // given
        Order order = buildOrder("tid-1");
        given(restTemplate.postForEntity(any(String.class), any(HttpEntity.class), eq(Map.class)))
                .willReturn(ResponseEntity.ok(Map.of("status", "SUCCESS_PAYMENT")));

        // when
        PaymentStatus result = kakaoPayService.inquire(order);

        // then
        assertThat(result).isEqualTo(PaymentStatus.PAID);
    }

    @Test
    @DisplayName("status가 READY면 아직 진행 중이므로 UNKNOWN으로 판단한다")
    void inquire_Ready_ReturnsUnknown() {
        // given
        Order order = buildOrder("tid-2");
        given(restTemplate.postForEntity(any(String.class), any(HttpEntity.class), eq(Map.class)))
                .willReturn(ResponseEntity.ok(Map.of("status", "READY")));

        // when
        PaymentStatus result = kakaoPayService.inquire(order);

        // then
        assertThat(result).isEqualTo(PaymentStatus.UNKNOWN);
    }

    @Test
    @DisplayName("status가 QUIT_PAYMENT/CANCEL_PAYMENT 등이면 NOT_PAID로 판단한다")
    void inquire_QuitPayment_ReturnsNotPaid() {
        // given
        Order order = buildOrder("tid-3");
        given(restTemplate.postForEntity(any(String.class), any(HttpEntity.class), eq(Map.class)))
                .willReturn(ResponseEntity.ok(Map.of("status", "QUIT_PAYMENT")));

        // when
        PaymentStatus result = kakaoPayService.inquire(order);

        // then
        assertThat(result).isEqualTo(PaymentStatus.NOT_PAID);
    }

    @Test
    @DisplayName("통신 자체가 실패하면 UNKNOWN으로 판단해 다음 주기에 재시도한다")
    void inquire_CommunicationError_ReturnsUnknown() {
        // given
        Order order = buildOrder("tid-4");
        given(restTemplate.postForEntity(any(String.class), any(HttpEntity.class), eq(Map.class)))
                .willThrow(new RuntimeException("네트워크 오류"));

        // when
        PaymentStatus result = kakaoPayService.inquire(order);

        // then
        assertThat(result).isEqualTo(PaymentStatus.UNKNOWN);
    }
}
