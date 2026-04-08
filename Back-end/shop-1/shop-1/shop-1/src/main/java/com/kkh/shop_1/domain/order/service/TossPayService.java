package com.kkh.shop_1.domain.order.service;

import com.kkh.shop_1.domain.order.dto.*;
import com.kkh.shop_1.domain.order.entity.PaymentType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
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
        String tossOrderId = "ORDER_" + req.getPartnerOrderId() + "_" + System.currentTimeMillis();
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
        params.put("orderId", req.getTid());
        params.put("amount", req.getAmount());

        log.info("[토스 승인 요청] orderId={}, amount={}, paymentKey={}", req.getTid(), req.getAmount(), req.getPaymentKey());

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(CONFIRM_URL, new HttpEntity<>(params, headers), Map.class);
            return PaymentApproveResponseDTO.builder().success(true).build();

        } catch (HttpClientErrorException e) {
            String errorBody = e.getResponseBodyAsString();

            // 🌟 복구됨: 중복 승인 요청 시 에러 뱉지 않고 조용히 성공 처리
            if (errorBody.contains("DUPLICATED_ORDER_ID")) {
                log.warn("▶ [토스 승인 무시] 이미 승인된 중복 요청입니다. (orderId={})", req.getTid());
                return PaymentApproveResponseDTO.builder().success(true).build();
            }

            log.error("토스 결제 승인 실패 (HTTP {}): {}", e.getStatusCode(), errorBody);
            throw new RuntimeException("토스 에러: " + errorBody);
        } catch (Exception e) {
            log.error("시스템 에러: {}", e.getMessage());
            throw new RuntimeException("결제 승인 중 알 수 없는 오류 발생", e);
        }
    }

    @Override
    public void cancel(String paymentKey, String cancelReason, Integer cancelAmount) {
        HttpHeaders headers = new HttpHeaders();
        String encodedAuth = Base64.getEncoder().encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));
        headers.set("Authorization", "Basic " + encodedAuth);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> params = new HashMap<>();
        params.put("cancelReason", cancelReason);
        if (cancelAmount != null) {
            params.put("cancelAmount", cancelAmount);
        }

        String cancelUrl = "https://api.tosspayments.com/v1/payments/" + paymentKey + "/cancel";

        try {
            restTemplate.postForEntity(cancelUrl, new HttpEntity<>(params, headers), Map.class);
            log.info("▶ [토스 결제 취소 완료] paymentKey={}, reason={}", paymentKey, cancelReason);

        } catch (HttpClientErrorException e) {
            String errorBody = e.getResponseBodyAsString();

            if (errorBody.contains("NOT_CANCELABLE_PAYMENT")) {
                log.warn("▶ [토스 취소 무시] 이미 취소되었거나 취소 불가능한 결제입니다. (paymentKey={})", paymentKey);
                return;
            }

            log.error("🚨 토스 결제 취소 실패: {}", errorBody);
            throw new RuntimeException("결제 취소 API 오류", e);

        } catch (Exception e) {
            log.error("🚨 토스 결제 취소 시스템 실패: {}", e.getMessage());
            throw new RuntimeException("결제 취소(보상 트랜잭션) 중 시스템 오류 발생", e);
        }
    }
}