package com.kkh.shop_1.domain.order.service;

import com.kkh.shop_1.domain.order.dto.PaymentApproveRequestDTO;
import com.kkh.shop_1.domain.order.dto.PaymentApproveResponseDTO;
import com.kkh.shop_1.domain.order.dto.PaymentReadyRequestDTO;
import com.kkh.shop_1.domain.order.dto.PaymentReadyResponseDTO;
import com.kkh.shop_1.domain.order.entity.PaymentType;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 *
 * 네이버페이 결제 서비스 구현체
 *
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NaverPayService implements PaymentService {

    private final RestTemplate restTemplate;

    @Value("${naverpay.client-id}")
    private String clientId;

    @Value("${naverpay.client-secret}")
    private String clientSecret;

    @Value("${naverpay.chain-id}")
    private String chainId;

    @Value("${naverpay.partner-id}")
    private String partnerId;

    private static final String API_URL = "https://dev-pay.paygate.naver.com";
    private static final String REDIRECT_BASE_URL = "https://test-pay.naver.com/payments/";

    @Override
    public PaymentType getPaymentType() {
        return PaymentType.NAVER_PAY;
    }

    /**
     *
     * 네이버페이 결제 예약 (Reserve API)
     *
     */
    @Override
    public PaymentReadyResponseDTO ready(PaymentReadyRequestDTO req) {
        String url = String.format("%s/%s/naverpay/payments/v2/reserve", API_URL, partnerId);

        HttpHeaders headers = createHeaders();
        NaverReadyRequest naverReq = NaverReadyRequest.of(req);
        HttpEntity<NaverReadyRequest> entity = new HttpEntity<>(naverReq, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            Map<String, Object> responseBody = response.getBody();

            if (isSuccess(responseBody)) {
                Map<String, Object> data = (Map<String, Object>) responseBody.get("body");
                String reserveId = (String) data.get("reserveId");

                log.info("NaverPay Reserve Success: ReserveID={}", reserveId);

                return PaymentReadyResponseDTO.builder()
                        .tid(reserveId)
                        .redirectUrl(REDIRECT_BASE_URL + reserveId)
                        .build();
            }

            log.error("NaverPay Reserve Failed: code={}, message={}",
                    responseBody.get("code"), responseBody.get("message"));
            throw new RuntimeException("네이버페이 결제 예약에 실패했습니다.");

        } catch (Exception e) {
            log.error("NaverPay API Communication Error: {}", e.getMessage());
            throw new RuntimeException("네이버페이 시스템 통신 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public PaymentApproveResponseDTO approve(PaymentApproveRequestDTO req) {
        log.info("NaverPay Approve logic called for ReserveID: {}", req.getTid());
        return PaymentApproveResponseDTO.builder().success(true).build();
    }

    // --- Private Helper Methods ---

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("X-Naver-Client-Id", clientId);
        headers.add("X-Naver-Client-Secret", clientSecret);
        headers.add("X-NaverPay-Chain-Id", chainId);
        return headers;
    }

    private boolean isSuccess(Map<String, Object> responseBody) {
        return responseBody != null && "Success".equals(responseBody.get("code"));
    }

    /**
     *
     * 네이버페이 API 전용 내부 요청 객체
     *
     */
    @Getter
    @Builder
    private static class NaverReadyRequest {
        private final String modelVersion = "2";
        private final String merchantPayKey;
        private final String merchantUserKey;
        private final String productName;
        private final Integer productCount;
        private final Integer totalPayAmount;
        private final Integer taxScopeAmount;
        private final Integer taxExScopeAmount = 0;
        private final String returnUrl;

        public static NaverReadyRequest of(PaymentReadyRequestDTO req) {
            return NaverReadyRequest.builder()
                    .merchantPayKey(req.getPartnerOrderId())
                    .merchantUserKey(req.getPartnerUserId())
                    .productName(req.getItemName())
                    .productCount(req.getQuantity())
                    .totalPayAmount(req.getTotalAmount())
                    .taxScopeAmount(req.getTotalAmount())
                    .returnUrl(req.getApprovalUrl())
                    .build();
        }
    }
}