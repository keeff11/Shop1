package com.kkh.shop_1.domain.order.service;

import com.kkh.shop_1.domain.order.dto.PaymentApproveRequestDTO;
import com.kkh.shop_1.domain.order.dto.PaymentApproveResponseDTO;
import com.kkh.shop_1.domain.order.dto.PaymentReadyRequestDTO;
import com.kkh.shop_1.domain.order.dto.PaymentReadyResponseDTO;
import com.kkh.shop_1.domain.order.entity.Order;
import com.kkh.shop_1.domain.order.entity.PaymentType;

public interface PaymentService {

    PaymentReadyResponseDTO ready(PaymentReadyRequestDTO request);

    PaymentApproveResponseDTO approve(PaymentApproveRequestDTO request);

    void cancel(String paymentKey, String cancelReason, Integer cancelAmount);

    /**
     * PG사 기준으로 이 주문의 실제 결제 상태를 조회한다.
     * (approve() 성공 직후 서버가 죽어 DB에 결제완료가 반영되지 못한 경우를 정합성 배치가 복구하는 데 사용)
     */
    PaymentStatus inquire(Order order);

    PaymentType getPaymentType();
}