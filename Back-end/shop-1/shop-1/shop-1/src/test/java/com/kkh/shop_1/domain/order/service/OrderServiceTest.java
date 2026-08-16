package com.kkh.shop_1.domain.order.service;

import com.kkh.shop_1.domain.item.entity.Item;
import com.kkh.shop_1.domain.item.entity.ItemCategory;
import com.kkh.shop_1.domain.item.entity.ItemStatus;
import com.kkh.shop_1.domain.order.dto.OrderApproveDTO;
import com.kkh.shop_1.domain.order.dto.OrderRequestDTO;
import com.kkh.shop_1.domain.order.dto.OrderResponseDTO;
import com.kkh.shop_1.domain.order.dto.PaymentApproveRequestDTO;
import com.kkh.shop_1.domain.order.dto.PaymentReadyResponseDTO;
import com.kkh.shop_1.domain.order.entity.Order;
import com.kkh.shop_1.domain.order.entity.OrderItem;
import com.kkh.shop_1.domain.order.entity.OrderStatus;
import com.kkh.shop_1.domain.order.entity.PaymentType;
import com.kkh.shop_1.domain.order.repository.OrderRepository;
import com.kkh.shop_1.domain.user.entity.Address;
import com.kkh.shop_1.domain.user.entity.User;
import com.kkh.shop_1.domain.user.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderServiceTest {

    @InjectMocks
    private OrderService orderService;

    @Mock
    private UserService userService;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentServiceFactory paymentServiceFactory;

    @Mock
    private OrderTxHandler orderTxHandler;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private PaymentService paymentService;

    private User user;
    private Address address;
    private Item item;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).nickname("testUser").build();
        address = Address.create(user, "12345", "Test Road", "101", "홍길동", "010-1234-5678");
        ReflectionTestUtils.setField(address, "id", 1L);
        item = Item.builder()
                .id(1L)
                .name("Test Item")
                .price(10000)
                .quantity(100)
                .itemCategory(ItemCategory.OTHERS)
                .status(ItemStatus.SELLING)
                .build();
    }

    @AfterEach
    void tearDown() {
        // InterruptedException 재현 테스트에서 세운 인터럽트 플래그가 다른 테스트로 새지 않도록 정리
        Thread.interrupted();
    }

    private Order buildOrder(Long orderId, int quantity) {
        Order order = Order.create(user, address);
        ReflectionTestUtils.setField(order, "id", orderId);
        order.addOrderItem(OrderItem.create(item, quantity));
        return order;
    }

    private OrderRequestDTO buildRequestDTO() {
        OrderRequestDTO.ItemOrder itemOrder = OrderRequestDTO.ItemOrder.builder()
                .itemId(1L)
                .quantity(2)
                .build();
        return OrderRequestDTO.builder()
                .addressId(1L)
                .itemOrders(Collections.singletonList(itemOrder))
                .paymentType(PaymentType.KAKAO_PAY)
                .approvalUrl("http://localhost/approve?orderId={orderId}")
                .cancelUrl("http://localhost/cancel")
                .failUrl("http://localhost/fail")
                .build();
    }

    @Nested
    @DisplayName("orderItems()")
    class OrderItemsTest {

        @Test
        @DisplayName("정상적으로 주문을 생성하고 결제를 준비한다")
        void orderItems_Success() throws InterruptedException {
            // given
            Long userId = 1L;
            OrderRequestDTO requestDTO = buildRequestDTO();
            Order order = buildOrder(100L, 2);

            RLock itemLock = mock(RLock.class);
            RLock multiLock = mock(RLock.class);
            given(redissonClient.getLock(anyString())).willReturn(itemLock);
            given(redissonClient.getMultiLock(any(RLock[].class))).willReturn(multiLock);
            given(multiLock.tryLock(10, 5, TimeUnit.SECONDS)).willReturn(true);

            given(userService.findById(userId)).willReturn(user);
            given(orderTxHandler.getOrCreateAddress(eq(user), eq(requestDTO))).willReturn(address);
            given(orderTxHandler.createOrder(eq(user), eq(address), eq(requestDTO))).willReturn(order);

            given(paymentServiceFactory.getService(PaymentType.KAKAO_PAY)).willReturn(paymentService);
            PaymentReadyResponseDTO readyResponse = PaymentReadyResponseDTO.builder()
                    .tid("tid-123")
                    .redirectUrl("http://redirect.url")
                    .build();
            given(paymentService.ready(any())).willReturn(readyResponse);

            // when
            OrderResponseDTO responseDTO = orderService.orderItems(userId, requestDTO);

            // then
            assertThat(responseDTO.getTid()).isEqualTo("tid-123");
            assertThat(responseDTO.getRedirectUrl()).isEqualTo("http://redirect.url");
            verify(multiLock).unlock();
            verify(orderTxHandler).updateTid(order.getId(), "tid-123");
        }

        @Test
        @DisplayName("상품을 찾을 수 없으면 예외가 전파된다")
        void orderItems_ItemNotFound_PropagatesException() throws InterruptedException {
            // given
            Long userId = 1L;
            OrderRequestDTO requestDTO = buildRequestDTO();

            RLock itemLock = mock(RLock.class);
            RLock multiLock = mock(RLock.class);
            given(redissonClient.getLock(anyString())).willReturn(itemLock);
            given(redissonClient.getMultiLock(any(RLock[].class))).willReturn(multiLock);
            given(multiLock.tryLock(10, 5, TimeUnit.SECONDS)).willReturn(true);

            given(userService.findById(userId)).willReturn(user);
            given(orderTxHandler.getOrCreateAddress(eq(user), eq(requestDTO))).willReturn(address);
            given(orderTxHandler.createOrder(eq(user), eq(address), eq(requestDTO)))
                    .willThrow(new IllegalArgumentException("상품 없음 ID: 1"));

            // when & then
            assertThatThrownBy(() -> orderService.orderItems(userId, requestDTO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("상품 없음");

            verify(multiLock).unlock();
        }

        @Test
        @DisplayName("Redisson 멀티락 획득에 실패하면 IllegalStateException을 던진다")
        void orderItems_MultiLockAcquireFailed_ThrowsIllegalStateException() throws InterruptedException {
            // given
            Long userId = 1L;
            OrderRequestDTO requestDTO = buildRequestDTO();

            RLock itemLock = mock(RLock.class);
            RLock multiLock = mock(RLock.class);
            given(redissonClient.getLock(anyString())).willReturn(itemLock);
            given(redissonClient.getMultiLock(any(RLock[].class))).willReturn(multiLock);
            given(multiLock.tryLock(10, 5, TimeUnit.SECONDS)).willReturn(false);

            given(userService.findById(userId)).willReturn(user);

            // when & then
            assertThatThrownBy(() -> orderService.orderItems(userId, requestDTO))
                    .isInstanceOf(IllegalStateException.class);

            verify(orderTxHandler, never()).createOrder(any(), any(), any());
            verify(multiLock, never()).unlock();
        }

        @Test
        @DisplayName("결제 준비 중 예외가 발생하면 주문을 취소하고 IllegalStateException을 던진다")
        void orderItems_PreparePaymentFails_CancelsOrderAndThrows() throws InterruptedException {
            // given
            Long userId = 1L;
            OrderRequestDTO requestDTO = buildRequestDTO();
            Order order = buildOrder(200L, 2);

            RLock itemLock = mock(RLock.class);
            RLock multiLock = mock(RLock.class);
            given(redissonClient.getLock(anyString())).willReturn(itemLock);
            given(redissonClient.getMultiLock(any(RLock[].class))).willReturn(multiLock);
            given(multiLock.tryLock(10, 5, TimeUnit.SECONDS)).willReturn(true);

            given(userService.findById(userId)).willReturn(user);
            given(orderTxHandler.getOrCreateAddress(eq(user), eq(requestDTO))).willReturn(address);
            given(orderTxHandler.createOrder(eq(user), eq(address), eq(requestDTO))).willReturn(order);

            given(paymentServiceFactory.getService(PaymentType.KAKAO_PAY)).willReturn(paymentService);
            given(paymentService.ready(any())).willThrow(new RuntimeException("PG 통신 실패"));

            // when & then
            assertThatThrownBy(() -> orderService.orderItems(userId, requestDTO))
                    .isInstanceOf(IllegalStateException.class);

            verify(orderTxHandler).cancelOrderPayment(order.getId());
            verify(orderTxHandler, never()).updateTid(anyLong(), anyString());
        }
    }

    @Nested
    @DisplayName("approveOrder() - 락 처리")
    class ApproveOrderLockTest {

        @Test
        @DisplayName("락 획득에 성공하면 실제 승인 로직을 위임하여 처리한다")
        void approveOrder_LockAcquired_DelegatesToDoApprove() throws InterruptedException {
            // given
            Long userId = 1L;
            Long orderId = 1L;
            OrderApproveDTO dto = new OrderApproveDTO();
            dto.setOrderId(orderId.toString());
            dto.setPg_token("pg-token");

            Order order = buildOrder(orderId, 1);
            ReflectionTestUtils.setField(order, "paymentType", PaymentType.KAKAO_PAY);
            ReflectionTestUtils.setField(order, "tid", "tid-1");

            RLock approveLock = mock(RLock.class);
            given(redissonClient.getLock("ORDER_APPROVE:" + orderId)).willReturn(approveLock);
            given(approveLock.tryLock(10, TimeUnit.SECONDS)).willReturn(true);

            given(orderRepository.findByIdWithFetch(orderId)).willReturn(Optional.of(order));
            given(paymentServiceFactory.getService(PaymentType.KAKAO_PAY)).willReturn(paymentService);

            // when
            orderService.approveOrder(dto, userId);

            // then
            verify(paymentService).approve(any(PaymentApproveRequestDTO.class));
            verify(orderTxHandler).completeOrderPayment(orderId, userId);
            verify(approveLock).unlock();
        }

        @Test
        @DisplayName("락 획득에 실패하면 IllegalStateException을 던진다")
        void approveOrder_LockNotAcquired_ThrowsIllegalStateException() throws InterruptedException {
            // given
            Long userId = 1L;
            Long orderId = 1L;
            OrderApproveDTO dto = new OrderApproveDTO();
            dto.setOrderId(orderId.toString());

            RLock approveLock = mock(RLock.class);
            given(redissonClient.getLock("ORDER_APPROVE:" + orderId)).willReturn(approveLock);
            given(approveLock.tryLock(10, TimeUnit.SECONDS)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> orderService.approveOrder(dto, userId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("결제 승인이 처리 중");

            verify(orderRepository, never()).findByIdWithFetch(anyLong());
            verify(approveLock, never()).unlock();
        }

        @Test
        @DisplayName("락 대기 중 인터럽트가 발생하면 인터럽트 상태를 복원하고 IllegalStateException을 던진다")
        void approveOrder_InterruptedDuringTryLock_ReInterruptsAndThrows() throws InterruptedException {
            // given
            Long userId = 1L;
            Long orderId = 1L;
            OrderApproveDTO dto = new OrderApproveDTO();
            dto.setOrderId(orderId.toString());

            RLock approveLock = mock(RLock.class);
            given(redissonClient.getLock("ORDER_APPROVE:" + orderId)).willReturn(approveLock);
            given(approveLock.tryLock(10, TimeUnit.SECONDS)).willThrow(new InterruptedException());

            // when & then
            assertThatThrownBy(() -> orderService.approveOrder(dto, userId))
                    .isInstanceOf(IllegalStateException.class);

            assertThat(Thread.currentThread().isInterrupted()).isTrue();
            verify(approveLock, never()).unlock();
        }
    }

    @Nested
    @DisplayName("approveOrder() - doApprove 내부 검증 로직")
    class DoApproveTest {

        private RLock stubApproveLock(Long orderId) throws InterruptedException {
            RLock approveLock = mock(RLock.class);
            given(redissonClient.getLock("ORDER_APPROVE:" + orderId)).willReturn(approveLock);
            given(approveLock.tryLock(10, TimeUnit.SECONDS)).willReturn(true);
            return approveLock;
        }

        @Test
        @DisplayName("이미 결제 완료(PAID)된 주문이면 PG 승인 호출 없이 바로 반환한다")
        void doApprove_AlreadyPaid_ShortCircuits() throws InterruptedException {
            // given
            Long userId = 1L;
            Long orderId = 1L;
            OrderApproveDTO dto = new OrderApproveDTO();
            dto.setOrderId(orderId.toString());

            Order order = buildOrder(orderId, 1);
            ReflectionTestUtils.setField(order, "status", OrderStatus.PAID);

            stubApproveLock(orderId);
            given(orderRepository.findByIdWithFetch(orderId)).willReturn(Optional.of(order));

            // when
            orderService.approveOrder(dto, userId);

            // then
            verify(paymentServiceFactory, never()).getService(any());
            verify(orderTxHandler, never()).completeOrderPayment(anyLong(), anyLong());
        }

        @Test
        @DisplayName("요청 금액이 서버가 계산한 주문 금액과 다르면 IllegalArgumentException을 던진다")
        void doApprove_AmountMismatch_ThrowsIllegalArgumentException() throws InterruptedException {
            // given
            Long userId = 1L;
            Long orderId = 1L;
            OrderApproveDTO dto = new OrderApproveDTO();
            dto.setOrderId(orderId.toString());
            dto.setAmount(999999); // 실제 주문 금액(10000 * 1)과 불일치

            Order order = buildOrder(orderId, 1);

            stubApproveLock(orderId);
            given(orderRepository.findByIdWithFetch(orderId)).willReturn(Optional.of(order));

            // when & then
            assertThatThrownBy(() -> orderService.approveOrder(dto, userId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("결제 금액이 주문 금액과 일치하지 않습니다.");

            verify(paymentServiceFactory, never()).getService(any());
        }

        @Test
        @DisplayName("요청 금액이 null이면 금액 검증을 건너뛰고 정상 승인한다")
        void doApprove_AmountNull_SkipsValidation() throws InterruptedException {
            // given
            Long userId = 1L;
            Long orderId = 1L;
            OrderApproveDTO dto = new OrderApproveDTO();
            dto.setOrderId(orderId.toString());
            dto.setAmount(null);

            Order order = buildOrder(orderId, 1);
            ReflectionTestUtils.setField(order, "paymentType", PaymentType.KAKAO_PAY);

            stubApproveLock(orderId);
            given(orderRepository.findByIdWithFetch(orderId)).willReturn(Optional.of(order));
            given(paymentServiceFactory.getService(PaymentType.KAKAO_PAY)).willReturn(paymentService);

            // when
            orderService.approveOrder(dto, userId);

            // then
            verify(paymentService).approve(any(PaymentApproveRequestDTO.class));
            verify(orderTxHandler).completeOrderPayment(orderId, userId);
        }

        @Test
        @DisplayName("승인 시 PG 요청 금액은 dto.getAmount()가 아니라 서버가 계산한 주문 총액을 사용한다")
        void doApprove_UsesServerComputedTotalAmount_NotClientAmount() throws InterruptedException {
            // given
            Long userId = 1L;
            Long orderId = 1L;
            OrderApproveDTO dto = new OrderApproveDTO();
            dto.setOrderId(orderId.toString());
            dto.setAmount(20000); // 실제 주문 금액과 우연히 같더라도 서버 값을 써야 함

            Order order = buildOrder(orderId, 2); // 10000 * 2 = 20000
            ReflectionTestUtils.setField(order, "paymentType", PaymentType.KAKAO_PAY);

            stubApproveLock(orderId);
            given(orderRepository.findByIdWithFetch(orderId)).willReturn(Optional.of(order));
            given(paymentServiceFactory.getService(PaymentType.KAKAO_PAY)).willReturn(paymentService);

            // when
            orderService.approveOrder(dto, userId);

            // then
            ArgumentCaptor<PaymentApproveRequestDTO> captor = ArgumentCaptor.forClass(PaymentApproveRequestDTO.class);
            verify(paymentService).approve(captor.capture());
            assertThat(captor.getValue().getAmount()).isEqualTo(order.getTotalAmount());
        }

        @Test
        @DisplayName("DB 결제 상태 업데이트 실패 시 서버 계산 금액으로 PG 취소를 호출하고 IllegalStateException을 던진다")
        void doApprove_CompletePaymentFails_CancelsWithTotalAmountAndThrows() throws InterruptedException {
            // given
            Long userId = 1L;
            Long orderId = 1L;
            OrderApproveDTO dto = new OrderApproveDTO();
            dto.setOrderId(orderId.toString());
            dto.setPaymentKey("payment-key-1");

            Order order = buildOrder(orderId, 2); // totalAmount = 20000
            ReflectionTestUtils.setField(order, "paymentType", PaymentType.KAKAO_PAY);

            stubApproveLock(orderId);
            given(orderRepository.findByIdWithFetch(orderId)).willReturn(Optional.of(order));
            given(paymentServiceFactory.getService(PaymentType.KAKAO_PAY)).willReturn(paymentService);
            willThrow(new RuntimeException("DB 업데이트 실패"))
                    .given(orderTxHandler).completeOrderPayment(orderId, userId);

            // when & then
            assertThatThrownBy(() -> orderService.approveOrder(dto, userId))
                    .isInstanceOf(IllegalStateException.class);

            verify(paymentService).cancel(eq("payment-key-1"), anyString(), eq(order.getTotalAmount()));
        }
    }
}
