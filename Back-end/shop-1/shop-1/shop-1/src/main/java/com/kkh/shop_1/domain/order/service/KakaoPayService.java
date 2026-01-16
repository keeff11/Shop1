package com.kkh.shop_1.domain.order.service;

import com.kkh.shop_1.domain.order.dto.PaymentApproveRequestDTO;
import com.kkh.shop_1.domain.order.dto.PaymentApproveResponseDTO;
import com.kkh.shop_1.domain.order.dto.PaymentReadyRequestDTO;
import com.kkh.shop_1.domain.order.dto.PaymentReadyResponseDTO;
import com.kkh.shop_1.domain.order.entity.PaymentType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoPayService implements PaymentService {

    @Value("${kakaopay.cid}")
    private String cid;

    @Value("${kakaopay.secret}")
    private String secretKey;

    private final RestTemplate restTemplate; // Bean 주입 권장

    private static final String READY_URL = "https://open-api.kakaopay.com/online/v1/payment/ready";
    private static final String APPROVE_URL = "https://open-api.kakaopay.com/online/v1/payment/approve";

    @Override
    public PaymentType getPaymentType() {
        return PaymentType.KAKAO_PAY;
    }

    /**
     *
     * 카카오페이 결제 준비 (Ready API)
     *
     */
    @Override
    public PaymentReadyResponseDTO ready(PaymentReadyRequestDTO req) {
        HttpHeaders headers = createHeaders();

        KakaoReadyRequest kakaoReq = KakaoReadyRequest.of(cid, req);
        HttpEntity<KakaoReadyRequest> entity = new HttpEntity<>(kakaoReq, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(READY_URL, entity, Map.class);
            Map<String, Object> body = response.getBody();

            if (body == null) throw new RuntimeException("카카오페이 응답이 비어있습니다.");

            log.info("KakaoPay Ready Success: TID={}", body.get("tid"));

            return PaymentReadyResponseDTO.builder()
                    .tid((String) body.get("tid"))
                    .redirectUrl(extractRedirectUrl(body))
                    .build();

        } catch (Exception e) {
            log.error("KakaoPay Ready Failed: {}", e.getMessage());
            throw new RuntimeException("결제 준비 중 오류가 발생했습니다.", e);
        }
    }

    /**
     *
     * 카카오페이 결제 승인 (Approve API)
     *
     */
    @Override
    public PaymentApproveResponseDTO approve(PaymentApproveRequestDTO req) {
        // 실제 승인 로직 구현 시에도 위와 유사하게 KakaoApproveRequest 등을 만들어 호출
        log.info("KakaoPay Approve logic called for TID: {}", req.getTid());
        return PaymentApproveResponseDTO.builder().success(true).build();
    }

    // --- Private Helper Methods ---

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "SECRET_KEY " + secretKey);
        return headers;
    }

    private String extractRedirectUrl(Map<String, Object> res) {
        String pcUrl = (String) res.get("next_redirect_pc_url");
        String mobileUrl = (String) res.get("next_redirect_mobile_url");
        return pcUrl != null ? pcUrl : mobileUrl;
    }

    /**
     *
     * 카카오페이 API 전용 내부 요청 객체
     *
     */
    @Getter
    @RequiredArgsConstructor(staticName = "of")
    private static class KakaoReadyRequest {
        private final String cid;
        private final String partner_order_id;
        private final String partner_user_id;
        private final String item_name;
        private final Integer quantity;
        private final Integer total_amount;
        private final Integer tax_free_amount = 0;
        private final String approval_url;
        private final String cancel_url;
        private final String fail_url;

        public static KakaoReadyRequest of(String cid, PaymentReadyRequestDTO req) {
            return new KakaoReadyRequest(
                    cid,
                    req.getPartnerOrderId(),
                    req.getPartnerUserId(),
                    req.getItemName(),
                    req.getQuantity(),
                    req.getTotalAmount(),
                    req.getApprovalUrl(),
                    req.getCancelUrl(),
                    req.getFailUrl()
            );
        }
    }
}