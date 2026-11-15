package com.kkh.shop_1.domain.order.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class OrderApproveDTO {

    private String orderId;
    private String pg_token;
    private String paymentKey; // 토스
    private String paymentId;  // 네이버 (returnUrl 콜백으로 전달됨)
    private Integer amount;
}