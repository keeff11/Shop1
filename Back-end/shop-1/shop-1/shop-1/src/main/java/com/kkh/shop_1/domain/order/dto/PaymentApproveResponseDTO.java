package com.kkh.shop_1.domain.order.dto;

import lombok.*;

@Getter
@Builder
public class PaymentApproveResponseDTO {
    private boolean success;
    private String message;
}

