package com.kkh.shop_1.domain.order.service;

import com.kkh.shop_1.domain.order.dto.PaymentApproveRequestDTO;
import com.kkh.shop_1.domain.order.dto.PaymentApproveResponseDTO;
import com.kkh.shop_1.domain.order.dto.PaymentReadyRequestDTO;
import com.kkh.shop_1.domain.order.dto.PaymentReadyResponseDTO;
import com.kkh.shop_1.domain.order.entity.Order;
import com.kkh.shop_1.domain.order.entity.PaymentType;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        String url = String.format("%s/%s/naverpay/payments/v2/apply/payment", API_URL, partnerId);

        HttpHeaders headers = createHeaders();
        Map<String, String> params = new HashMap<>();
        params.put("paymentId", req.getTid());
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            Map<String, Object> responseBody = response.getBody();

            if (isSuccess(responseBody)) {
                Map<String, Object> data = (Map<String, Object>) responseBody.get("body");
                Map<String, Object> detail = (Map<String, Object>) data.get("detail");
                String admissionState = detail != null ? (String) detail.get("admissionState") : null;

                if (!"SUCCESS".equals(admissionState)) {
                    log.error("NaverPay Approve Not Admitted: ReserveID={}, admissionState={}", req.getTid(), admissionState);
                    throw new RuntimeException("네이버페이 결제 승인이 완료되지 않았습니다.");
                }

                log.info("NaverPay Approve Success: ReserveID={}, PaymentId={}", req.getTid(), data.get("paymentId"));
                return PaymentApproveResponseDTO.builder().success(true).build();
            }

            log.error("NaverPay Approve Failed: code={}, message={}",
                    responseBody != null ? responseBody.get("code") : null,
                    responseBody != null ? responseBody.get("message") : null);
            throw new RuntimeException("네이버페이 결제 승인에 실패했습니다.");

        } catch (Exception e) {
            log.error("NaverPay Approve Communication Error: {}", e.getMessage());
            throw new RuntimeException("네이버페이 결제 승인 처리 중 오류가 발생했습니다.", e);
        }
    }
    
    /**
     * 네이버페이 결제 조회. approve() 호출 직후 서버가 죽어 우리 DB에 반영되지 못한 결제를
     * 정합성 배치가 나중에 복구할 수 있도록 실제 네이버 쪽 상태를 확인한다.
     *
     * 실제 호출로 확인함: 이전에 썼던 v2/find는 존재하지 않는 경로였다(순수 404, API 자체가 없음).
     * v2/list/history가 실제 존재하는 엔드포인트다 (더미 paymentId로도 정상적인 JSON 에러 구조
     * {"code":"InvalidPaymentId", "body":{"list":[],...}} 로 응답하는 것으로 확인).
     *
     * ⚠️ 주의: 단건 조회가 아니라 목록(list) 조회 응답 구조라 body.list[0] 안에서 상태를 찾도록
     * 작성했지만, 실제로 승인 완료된 결제 건으로 "성공" 응답까지는 확인하지 못했다(테스트 결제를
     * 끝까지 완료하지 못함). 실 연동 전에 반드시 완료된 결제 건으로 응답 필드를 한 번 더 확인해야 한다.
     */
    @Override
    public PaymentStatus inquire(Order order) {
        String url = String.format("%s/%s/naverpay/payments/v2/list/history", API_URL, partnerId);

        HttpHeaders headers = createHeaders();
        Map<String, String> params = new HashMap<>();
        params.put("paymentId", order.getTid());
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            Map<String, Object> responseBody = response.getBody();

            if (!isSuccess(responseBody)) {
                // InvalidPaymentId 등: 승인된 적 없는 결제로 간주
                log.warn("NaverPay Inquire Failed: reserveId={}, body={}", order.getTid(), responseBody);
                return PaymentStatus.NOT_PAID;
            }

            Map<String, Object> data = (Map<String, Object>) responseBody.get("body");
            List<Map<String, Object>> list = data != null ? (List<Map<String, Object>>) data.get("list") : null;

            if (list == null || list.isEmpty()) {
                return PaymentStatus.NOT_PAID;
            }

            String admissionState = (String) list.get(0).get("admissionState");
            if ("SUCCESS".equals(admissionState)) {
                return PaymentStatus.PAID;
            }
            if (admissionState == null) {
                return PaymentStatus.UNKNOWN;
            }
            return PaymentStatus.NOT_PAID;

        } catch (Exception e) {
            log.error("NaverPay Inquire Communication Error: {}", e.getMessage());
            return PaymentStatus.UNKNOWN;
        }
    }

    @Override
    public void cancel(String paymentKey, String cancelReason, Integer cancelAmount) {
        String cancelUrl = String.format("%s/%s/naverpay/payments/v1/cancel", API_URL, partnerId);

        HttpHeaders headers = createHeaders();
        Map<String, Object> params = new HashMap<>();
        params.put("paymentId", paymentKey); // 승인된 네이버페이 결제번호
        params.put("cancelAmount", cancelAmount);
        params.put("cancelReason", cancelReason);
        params.put("cancelRequester", 1);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(cancelUrl, entity, Map.class);
            Map<String, Object> responseBody = response.getBody();

            if (isSuccess(responseBody)) {
                log.info("▶ [네이버페이 결제 취소 완료] paymentId={}, reason={}", paymentKey, cancelReason);
            } else {
                log.error("네이버페이 결제 취소 응답 오류: {}", responseBody);
                throw new RuntimeException("네이버페이 결제 취소 처리가 실패했습니다.");
            }
        } catch (Exception e) {
            log.error("네이버페이 결제 취소 통신 실패: {}", e.getMessage());
            throw new RuntimeException("네이버페이 결제 취소(보상 트랜잭션) 중 오류 발생", e);
        }
    }

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