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
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class TossPayServiceInquireTest {

    @InjectMocks
    private TossPayService tossPayService;

    @Mock
    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(tossPayService, "secretKey", "test_sk_dummy");
    }

    private Order buildOrder(String tid) {
        Order order = Order.create(mock(User.class), mock(Address.class));
        ReflectionTestUtils.setField(order, "tid", tid);
        return order;
    }

    @Test
    @DisplayName("status가 DONE이면 PAID로 판단한다")
    void inquire_Done_ReturnsPaid() {
        // given
        Order order = buildOrder("ORDER_123");
        Map<String, Object> body = Map.of("status", "DONE");
        given(restTemplate.exchange(eq("https://api.tosspayments.com/v1/payments/orders/ORDER_123"),
                eq(HttpMethod.GET), any(), eq(Map.class)))
                .willReturn(ResponseEntity.ok(body));

        // when
        PaymentStatus result = tossPayService.inquire(order);

        // then
        assertThat(result).isEqualTo(PaymentStatus.PAID);
    }

    @Test
    @DisplayName("status가 CANCELED/EXPIRED 등이면 NOT_PAID로 판단한다")
    void inquire_Canceled_ReturnsNotPaid() {
        // given
        Order order = buildOrder("ORDER_456");
        Map<String, Object> body = Map.of("status", "CANCELED");
        given(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), any(), eq(Map.class)))
                .willReturn(ResponseEntity.ok(body));

        // when
        PaymentStatus result = tossPayService.inquire(order);

        // then
        assertThat(result).isEqualTo(PaymentStatus.NOT_PAID);
    }

    @Test
    @DisplayName("status가 READY/IN_PROGRESS 등 진행 중이면 UNKNOWN으로 판단해 다음 주기로 넘긴다")
    void inquire_InProgress_ReturnsUnknown() {
        // given
        Order order = buildOrder("ORDER_789");
        Map<String, Object> body = Map.of("status", "IN_PROGRESS");
        given(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), any(), eq(Map.class)))
                .willReturn(ResponseEntity.ok(body));

        // when
        PaymentStatus result = tossPayService.inquire(order);

        // then
        assertThat(result).isEqualTo(PaymentStatus.UNKNOWN);
    }

    @Test
    @DisplayName("토스에 해당 orderId 자체가 없으면(404) 결제된 적 없는 것으로 판단한다")
    void inquire_NotFound_ReturnsNotPaid() {
        // given
        Order order = buildOrder("ORDER_NEVER_APPROVED");
        given(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), any(), eq(Map.class)))
                .willThrow(HttpClientErrorException.NotFound.create(HttpStatus.NOT_FOUND, "Not Found", null, null, null));

        // when
        PaymentStatus result = tossPayService.inquire(order);

        // then
        assertThat(result).isEqualTo(PaymentStatus.NOT_PAID);
    }

    @Test
    @DisplayName("통신 자체가 실패하면 UNKNOWN으로 판단해 다음 주기에 재시도한다")
    void inquire_CommunicationError_ReturnsUnknown() {
        // given
        Order order = buildOrder("ORDER_ERR");
        given(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), any(), eq(Map.class)))
                .willThrow(new RuntimeException("네트워크 오류"));

        // when
        PaymentStatus result = tossPayService.inquire(order);

        // then
        assertThat(result).isEqualTo(PaymentStatus.UNKNOWN);
    }
}
