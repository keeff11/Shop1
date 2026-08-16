package com.kkh.shop_1.domain.order.service;

/**
 *
 * PG사에 실제로 결제가 이루어졌는지 조회한 결과.
 * (우리 DB의 Order.status와는 별개로, PG사 기준의 진실을 나타낸다)
 *
 */
public enum PaymentStatus {
    PAID,
    NOT_PAID,
    /** 조회 자체가 실패했거나, 아직 결론이 나지 않은 상태(예: 결제 진행 중) */
    UNKNOWN
}
