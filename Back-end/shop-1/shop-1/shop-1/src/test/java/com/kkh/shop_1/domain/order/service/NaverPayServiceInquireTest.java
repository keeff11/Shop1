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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class NaverPayServiceInquireTest {

    @InjectMocks
    private NaverPayService naverPayService;

    @Mock
    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(naverPayService, "clientId", "test_client_id");
        ReflectionTestUtils.setField(naverPayService, "clientSecret", "test_client_secret");
        ReflectionTestUtils.setField(naverPayService, "chainId", "test_chain_id");
        ReflectionTestUtils.setField(naverPayService, "partnerId", "test_partner_id");
    }

    private Order buildOrder(String tid) {
        Order order = Order.create(mock(User.class), mock(Address.class));
        ReflectionTestUtils.setField(order, "tid", tid);
        return order;
    }

    private Map<String, Object> successBody(List<Map<String, Object>> list) {
        Map<String, Object> body = new HashMap<>();
        body.put("list", list);
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("code", "Success");
        responseBody.put("body", body);
        return responseBody;
    }

    @Test
    @DisplayName("isSuccess=true이고 admissionState가 SUCCESS면 PAID로 판단한다")
    void inquire_SuccessAndAdmissionStateSuccess_ReturnsPaid() {
        // given
        Order order = buildOrder("reserve-1");
        Map<String, Object> item = new HashMap<>();
        item.put("admissionState", "SUCCESS");
        given(restTemplate.postForEntity(any(String.class), any(HttpEntity.class), eq(Map.class)))
                .willReturn(ResponseEntity.ok(successBody(List.of(item))));

        // when
        PaymentStatus result = naverPayService.inquire(order);

        // then
        assertThat(result).isEqualTo(PaymentStatus.PAID);
    }

    @Test
    @DisplayName("isSuccess=false이면(예: InvalidPaymentId) NOT_PAID로 판단한다")
    void inquire_NotSuccess_ReturnsNotPaid() {
        // given
        Order order = buildOrder("reserve-2");
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("code", "InvalidPaymentId");
        responseBody.put("message", "존재하지 않는 결제건입니다.");
        given(restTemplate.postForEntity(any(String.class), any(HttpEntity.class), eq(Map.class)))
                .willReturn(ResponseEntity.ok(responseBody));

        // when
        PaymentStatus result = naverPayService.inquire(order);

        // then
        assertThat(result).isEqualTo(PaymentStatus.NOT_PAID);
    }

    @Test
    @DisplayName("list가 비어있으면 NOT_PAID로 판단한다")
    void inquire_EmptyList_ReturnsNotPaid() {
        // given
        Order order = buildOrder("reserve-3");
        given(restTemplate.postForEntity(any(String.class), any(HttpEntity.class), eq(Map.class)))
                .willReturn(ResponseEntity.ok(successBody(List.of())));

        // when
        PaymentStatus result = naverPayService.inquire(order);

        // then
        assertThat(result).isEqualTo(PaymentStatus.NOT_PAID);
    }

    @Test
    @DisplayName("list가 없으면(null) NOT_PAID로 판단한다")
    void inquire_NullList_ReturnsNotPaid() {
        // given
        Order order = buildOrder("reserve-4");
        Map<String, Object> body = new HashMap<>(); // "list" 키 자체가 없음
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("code", "Success");
        responseBody.put("body", body);
        given(restTemplate.postForEntity(any(String.class), any(HttpEntity.class), eq(Map.class)))
                .willReturn(ResponseEntity.ok(responseBody));

        // when
        PaymentStatus result = naverPayService.inquire(order);

        // then
        assertThat(result).isEqualTo(PaymentStatus.NOT_PAID);
    }

    @Test
    @DisplayName("admissionState가 null이면 UNKNOWN으로 판단한다")
    void inquire_AdmissionStateNull_ReturnsUnknown() {
        // given
        Order order = buildOrder("reserve-5");
        Map<String, Object> item = new HashMap<>(); // "admissionState" 키 자체가 없음
        given(restTemplate.postForEntity(any(String.class), any(HttpEntity.class), eq(Map.class)))
                .willReturn(ResponseEntity.ok(successBody(List.of(item))));

        // when
        PaymentStatus result = naverPayService.inquire(order);

        // then
        assertThat(result).isEqualTo(PaymentStatus.UNKNOWN);
    }

    @Test
    @DisplayName("admissionState가 SUCCESS가 아닌 다른 값이면 NOT_PAID로 판단한다")
    void inquire_OtherAdmissionState_ReturnsNotPaid() {
        // given
        Order order = buildOrder("reserve-6");
        Map<String, Object> item = new HashMap<>();
        item.put("admissionState", "CANCEL");
        given(restTemplate.postForEntity(any(String.class), any(HttpEntity.class), eq(Map.class)))
                .willReturn(ResponseEntity.ok(successBody(List.of(item))));

        // when
        PaymentStatus result = naverPayService.inquire(order);

        // then
        assertThat(result).isEqualTo(PaymentStatus.NOT_PAID);
    }

    @Test
    @DisplayName("통신 자체가 실패하면 UNKNOWN으로 판단해 다음 주기에 재시도한다")
    void inquire_CommunicationError_ReturnsUnknown() {
        // given
        Order order = buildOrder("reserve-7");
        given(restTemplate.postForEntity(any(String.class), any(HttpEntity.class), eq(Map.class)))
                .willThrow(new RuntimeException("네트워크 오류"));

        // when
        PaymentStatus result = naverPayService.inquire(order);

        // then
        assertThat(result).isEqualTo(PaymentStatus.UNKNOWN);
    }
}
