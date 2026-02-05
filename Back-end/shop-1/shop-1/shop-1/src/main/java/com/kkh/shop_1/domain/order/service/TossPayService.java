package com.kkh.shop_1.domain.order.service;

import com.kkh.shop_1.domain.order.dto.*;
import com.kkh.shop_1.domain.order.entity.PaymentType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException; // 추가
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TossPayService implements PaymentService {

    @Value("${toss.secret}")
    private String secretKey;

    private final RestTemplate restTemplate;
    private static final String CONFIRM_URL = "https://api.tosspayments.com/v1/payments/confirm";

    @Override
    public PaymentType getPaymentType() {
        return PaymentType.TOSS_PAY;
    }

    @Override
    public PaymentReadyResponseDTO ready(PaymentReadyRequestDTO req) {
        String tossOrderId = "ORDER_" + req.getPartnerOrderId();
        log.info("TossPay Ready: Generated TossOrderId={}", tossOrderId);
        return PaymentReadyResponseDTO.builder()
                .tid(tossOrderId)
                .redirectUrl(null)
                .build();
    }

    @Override
    public PaymentApproveResponseDTO approve(PaymentApproveRequestDTO req) {
        HttpHeaders headers = new HttpHeaders();
        String encodedAuth = Base64.getEncoder().encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));
        headers.set("Authorization", "Basic " + encodedAuth);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> params = new HashMap<>();
        params.put("paymentKey", req.getPaymentKey());
        params.put("orderId", req.getTid()); // "ORDER_..." 형식이어야 함
        params.put("amount", req.getAmount());

        log.info("▶ [토스 승인 요청] orderId={}, amount={}, paymentKey={}", req.getTid(), req.getAmount(), req.getPaymentKey());

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(CONFIRM_URL, new HttpEntity<>(params, headers), Map.class);
            return PaymentApproveResponseDTO.builder().success(true).build();

        } catch (HttpClientErrorException e) {
            // [핵심] 토스 서버가 보낸 에러 메시지를 로그에 출력
            log.error("🚨 토스 결제 승인 실패 (HTTP {}): {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("토스 에러: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("🚨 시스템 에러: {}", e.getMessage());
            throw new RuntimeException("결제 승인 중 알 수 없는 오류 발생", e);
        }
    }
}