package com.kkh.shop_1.domain.order.service;

import com.kkh.shop_1.domain.order.dto.PaymentApproveRequestDTO;
import com.kkh.shop_1.domain.order.dto.PaymentApproveResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class NaverPayServiceApproveTest {

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

    private PaymentApproveRequestDTO request(String paymentId) {
        return PaymentApproveRequestDTO.builder()
                .tid("reserve-1")
                .partnerOrderId("10")
                .partnerUserId("1")
                .paymentId(paymentId)
                .amount(5000)
                .build();
    }

    private Map<String, Object> approveBody(String merchantPayKey) {
        Map<String, Object> detail = new HashMap<>();
        detail.put("admissionState", "SUCCESS");
        detail.put("merchantPayKey", merchantPayKey);
        detail.put("totalPayAmount", 5000);
        Map<String, Object> body = new HashMap<>();
        body.put("paymentId", "pay-1");
        body.put("detail", detail);
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("code", "Success");
        responseBody.put("body", body);
        return responseBody;
    }

    @Test
    @DisplayName("ready의 reserveId(tid)가 아니라 콜백으로 받은 paymentId로 승인을 요청하고, 그 값을 취소 키로 돌려준다")
    void approve_UsesCallbackPaymentId() {
        given(restTemplate.postForEntity(contains("/apply/payment"), any(HttpEntity.class), eq(Map.class)))
                .willReturn(ResponseEntity.ok(approveBody("10")));

        PaymentApproveResponseDTO result = naverPayService.approve(request("pay-1"));

        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(contains("/apply/payment"), captor.capture(), eq(Map.class));
        assertThat((Map<String, String>) captor.getValue().getBody()).containsEntry("paymentId", "pay-1");
        assertThat(result.getCancelKey()).isEqualTo("pay-1");
    }

    @Test
    @DisplayName("paymentId가 없으면 네이버 API를 호출하지 않고 예외를 던진다")
    void approve_MissingPaymentId_Throws() {
        assertThatThrownBy(() -> naverPayService.approve(request(null)))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(restTemplate);
    }

    @Test
    @DisplayName("승인된 결제의 merchantPayKey가 주문 ID와 다르면 그 결제를 취소하고 예외를 던진다")
    void approve_MerchantPayKeyMismatch_CancelsAndThrows() {
        given(restTemplate.postForEntity(contains("/apply/payment"), any(HttpEntity.class), eq(Map.class)))
                .willReturn(ResponseEntity.ok(approveBody("99")));
        Map<String, Object> cancelOk = new HashMap<>();
        cancelOk.put("code", "Success");
        given(restTemplate.postForEntity(contains("/cancel"), any(HttpEntity.class), eq(Map.class)))
                .willReturn(ResponseEntity.ok(cancelOk));

        assertThatThrownBy(() -> naverPayService.approve(request("pay-1")))
                .isInstanceOf(RuntimeException.class);

        verify(restTemplate).postForEntity(contains("/cancel"), any(HttpEntity.class), eq(Map.class));
    }

    @Test
    @DisplayName("merchantPayKey가 주문 ID와 같으면 취소하지 않는다")
    void approve_MerchantPayKeyMatch_DoesNotCancel() {
        given(restTemplate.postForEntity(contains("/apply/payment"), any(HttpEntity.class), eq(Map.class)))
                .willReturn(ResponseEntity.ok(approveBody("10")));

        naverPayService.approve(request("pay-1"));

        verify(restTemplate, never()).postForEntity(contains("/cancel"), any(HttpEntity.class), eq(Map.class));
    }
}
