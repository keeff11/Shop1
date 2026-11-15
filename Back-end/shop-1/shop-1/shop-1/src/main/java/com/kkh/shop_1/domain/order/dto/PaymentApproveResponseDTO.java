package com.kkh.shop_1.domain.order.dto;

import lombok.*;

@Getter
@Builder
public class PaymentApproveResponseDTO {
    private boolean success;
    private String message;

    // 승인된 결제를 취소할 때 PG에 넘길 키. PG마다 달라서(카카오: tid, 토스: paymentKey, 네이버: paymentId) 구현체가 채운다.
    private String cancelKey;
}

