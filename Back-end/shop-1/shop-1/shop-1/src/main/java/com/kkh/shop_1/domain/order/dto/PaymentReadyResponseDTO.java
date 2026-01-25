package com.kkh.shop_1.domain.order.dto;

import lombok.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentReadyResponseDTO {
    private String tid;          // 카카오: tid, 네이버: paymentId
    private String redirectUrl;  // 결제 페이지 URL
}
