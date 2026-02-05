package com.kkh.shop_1.domain.order.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class OrderApproveDTO {

    private String orderId;
    private String pg_token;
    private String paymentKey;
    private Integer amount;
}