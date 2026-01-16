package com.kkh.shop_1.domain.order.service;

import com.kkh.shop_1.domain.order.dto.PaymentApproveRequestDTO;
import com.kkh.shop_1.domain.order.dto.PaymentApproveResponseDTO;
import com.kkh.shop_1.domain.order.dto.PaymentReadyRequestDTO;
import com.kkh.shop_1.domain.order.dto.PaymentReadyResponseDTO;
import com.kkh.shop_1.domain.order.entity.PaymentType;

public interface PaymentService {

    PaymentReadyResponseDTO ready(PaymentReadyRequestDTO request);

    PaymentApproveResponseDTO approve(PaymentApproveRequestDTO request);

    PaymentType getPaymentType();
}