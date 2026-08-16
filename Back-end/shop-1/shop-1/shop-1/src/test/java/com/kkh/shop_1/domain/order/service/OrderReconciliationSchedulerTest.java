package com.kkh.shop_1.domain.order.service;

import com.kkh.shop_1.domain.order.entity.Order;
import com.kkh.shop_1.domain.order.entity.OrderStatus;
import com.kkh.shop_1.domain.order.entity.PaymentType;
import com.kkh.shop_1.domain.order.repository.OrderRepository;
import com.kkh.shop_1.domain.user.entity.Address;
import com.kkh.shop_1.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderReconciliationSchedulerTest {

    @InjectMocks
    private OrderReconciliationScheduler scheduler;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderTxHandler orderTxHandler;

    @Mock
    private PaymentServiceFactory paymentServiceFactory;

    @Mock
    private PaymentService paymentService;

    private Order buildStuckOrder(Long orderId, Long userId) {
        User user = mock(User.class);
        given(user.getId()).willReturn(userId);

        Order order = Order.create(user, mock(Address.class));
        ReflectionTestUtils.setField(order, "id", orderId);
        ReflectionTestUtils.setField(order, "tid", "tid-" + orderId);
        ReflectionTestUtils.setField(order, "paymentType", PaymentType.TOSS_PAY);
        return order;
    }

    @Test
    @DisplayName("PG는 결제 완료인데 DB는 PAYMENT_PENDING인 주문은 결제 완료로 복구 처리한다")
    void reconcile_PgPaid_CompletesPayment() {
        // given
        Order order = buildStuckOrder(1L, 100L);
        given(orderRepository.findByStatusAndOrderDateBefore(eq(OrderStatus.PAYMENT_PENDING), any(LocalDateTime.class)))
                .willReturn(List.of(order));
        given(paymentServiceFactory.getService(PaymentType.TOSS_PAY)).willReturn(paymentService);
        given(paymentService.inquire(order)).willReturn(PaymentStatus.PAID);

        // when
        scheduler.reconcileStuckOrders();

        // then
        verify(orderTxHandler).completeOrderPayment(1L, 100L);
        verify(orderTxHandler, never()).cancelOrderPayment(anyLong());
    }

    @Test
    @DisplayName("PG에도 결제되지 않은 주문은 취소 처리(재고 복구)한다")
    void reconcile_PgNotPaid_CancelsOrder() {
        // given
        Order order = buildStuckOrder(2L, 100L);
        given(orderRepository.findByStatusAndOrderDateBefore(eq(OrderStatus.PAYMENT_PENDING), any(LocalDateTime.class)))
                .willReturn(List.of(order));
        given(paymentServiceFactory.getService(PaymentType.TOSS_PAY)).willReturn(paymentService);
        given(paymentService.inquire(order)).willReturn(PaymentStatus.NOT_PAID);

        // when
        scheduler.reconcileStuckOrders();

        // then
        verify(orderTxHandler).cancelOrderPayment(2L);
        verify(orderTxHandler, never()).completeOrderPayment(anyLong(), anyLong());
    }

    @Test
    @DisplayName("PG 상태를 확정할 수 없으면(UNKNOWN) 아무 것도 하지 않고 다음 주기로 넘긴다")
    void reconcile_PgUnknown_DoesNothing() {
        // given
        Order order = buildStuckOrder(3L, 100L);
        given(orderRepository.findByStatusAndOrderDateBefore(eq(OrderStatus.PAYMENT_PENDING), any(LocalDateTime.class)))
                .willReturn(List.of(order));
        given(paymentServiceFactory.getService(PaymentType.TOSS_PAY)).willReturn(paymentService);
        given(paymentService.inquire(order)).willReturn(PaymentStatus.UNKNOWN);

        // when
        scheduler.reconcileStuckOrders();

        // then
        verify(orderTxHandler, never()).completeOrderPayment(anyLong(), anyLong());
        verify(orderTxHandler, never()).cancelOrderPayment(anyLong());
    }

    @Test
    @DisplayName("한 주문 처리 중 예외가 나도 다른 주문 처리에는 영향을 주지 않는다")
    void reconcile_OneOrderFails_OthersStillProcessed() {
        // given
        Order failingOrder = buildStuckOrder(4L, 100L);
        Order healthyOrder = buildStuckOrder(5L, 200L);
        given(orderRepository.findByStatusAndOrderDateBefore(eq(OrderStatus.PAYMENT_PENDING), any(LocalDateTime.class)))
                .willReturn(List.of(failingOrder, healthyOrder));
        given(paymentServiceFactory.getService(PaymentType.TOSS_PAY)).willReturn(paymentService);
        given(paymentService.inquire(failingOrder)).willThrow(new RuntimeException("PG 통신 실패"));
        given(paymentService.inquire(healthyOrder)).willReturn(PaymentStatus.PAID);

        // when
        scheduler.reconcileStuckOrders();

        // then
        verify(orderTxHandler).completeOrderPayment(5L, 200L);
        verify(orderTxHandler, never()).completeOrderPayment(eq(4L), any());
    }

    @Test
    @DisplayName("방치된 주문이 없으면 PG 조회를 시도하지 않는다")
    void reconcile_NoStuckOrders_DoesNotCallPg() {
        // given
        given(orderRepository.findByStatusAndOrderDateBefore(eq(OrderStatus.PAYMENT_PENDING), any(LocalDateTime.class)))
                .willReturn(List.of());

        // when
        scheduler.reconcileStuckOrders();

        // then
        verify(paymentServiceFactory, never()).getService(any());
    }
}
