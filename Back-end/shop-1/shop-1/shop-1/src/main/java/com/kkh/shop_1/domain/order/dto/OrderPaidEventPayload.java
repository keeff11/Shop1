package com.kkh.shop_1.domain.order.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 *
 * 결제 완료 outbox 이벤트의 payload. (알림 발송 등 트랜잭션 밖 부수 작업에 필요한 최소 정보)
 *
 */
@Getter
@Builder
public class OrderPaidEventPayload {
    private Long orderId;
    private Long userId;
    private int totalAmount;
    private LocalDateTime paidAt;
}
