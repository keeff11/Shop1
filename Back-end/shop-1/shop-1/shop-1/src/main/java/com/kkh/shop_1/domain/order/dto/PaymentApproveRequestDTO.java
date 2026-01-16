package com.kkh.shop_1.domain.order.dto;

import com.kkh.shop_1.domain.order.entity.PaymentType;
import lombok.*;

@Getter
@Builder
public class PaymentApproveRequestDTO {
    private PaymentType paymentType;

    private String tid;
    private String partnerOrderId;
    private String partnerUserId;
    private String pgToken; // 카카오
    private String paymentId; // 네이버
}

