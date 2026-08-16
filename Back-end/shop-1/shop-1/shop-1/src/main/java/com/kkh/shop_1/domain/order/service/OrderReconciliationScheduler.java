package com.kkh.shop_1.domain.order.service;

import com.kkh.shop_1.domain.order.entity.Order;
import com.kkh.shop_1.domain.order.entity.OrderStatus;
import com.kkh.shop_1.domain.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 *
 * PG사 승인(approve)은 성공했지만, 그 직후 서버 장애 등으로 우리 DB에 결제 완료가 반영되지 못한 채
 * PAYMENT_PENDING 상태로 방치된 주문을 찾아 PG사 기준의 실제 상태로 맞춰주는 정합성 배치.
 *
 * PG 상태 조회(외부 I/O)는 트랜잭션 밖에서 수행하고, DB 반영(완료 처리/취소 처리)만 각각 짧은
 * 트랜잭션으로 처리한다 — 동시성 문제 해결 때와 같은 이유로, 외부 호출을 트랜잭션 안에 두지 않는다.
 *
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderReconciliationScheduler {

    // 정상적인 승인 처리가 이보다 오래 걸리진 않는다고 보고, 이 시간을 넘겨 PAYMENT_PENDING인 주문만 대상으로 한다.
    private static final Duration STALE_THRESHOLD = Duration.ofMinutes(10);

    private final OrderRepository orderRepository;
    private final OrderTxHandler orderTxHandler;
    private final PaymentServiceFactory paymentServiceFactory;

    @Scheduled(fixedDelay = 5 * 60 * 1000L) // 5분마다
    public void reconcileStuckOrders() {
        LocalDateTime threshold = LocalDateTime.now().minus(STALE_THRESHOLD);
        List<Order> stuckOrders = orderRepository.findByStatusAndOrderDateBefore(OrderStatus.PAYMENT_PENDING, threshold);

        if (stuckOrders.isEmpty()) {
            return;
        }

        log.warn("결제 정합성 점검: PAYMENT_PENDING으로 {}분 이상 방치된 주문 {}건 발견", STALE_THRESHOLD.toMinutes(), stuckOrders.size());
        stuckOrders.forEach(this::reconcileOrder);
    }

    private void reconcileOrder(Order order) {
        try {
            PaymentStatus pgStatus = paymentServiceFactory.getService(order.getPaymentType()).inquire(order);

            switch (pgStatus) {
                case PAID -> {
                    log.warn("PG는 결제 완료 상태인데 DB엔 반영되지 않은 주문을 발견하여 복구 처리합니다. orderId={}", order.getId());
                    orderTxHandler.completeOrderPayment(order.getId(), order.getUser().getId());
                }
                case NOT_PAID -> {
                    log.info("PG에도 결제되지 않은 주문입니다. 재고를 복구하고 주문을 취소 처리합니다. orderId={}", order.getId());
                    orderTxHandler.cancelOrderPayment(order.getId());
                }
                case UNKNOWN -> log.error("PG 결제 상태를 확정할 수 없습니다. 다음 주기에 재시도합니다. orderId={}", order.getId());
            }
        } catch (Exception e) {
            // 한 건 처리 중 오류가 나도 나머지 주문 처리에 영향이 없도록 여기서 잡는다.
            log.error("주문 정합성 점검 중 오류가 발생했습니다. orderId={}", order.getId(), e);
        }
    }
}
