package com.kkh.shop_1.domain.order.service;

import com.kkh.shop_1.domain.cart.service.CartItemService;
import com.kkh.shop_1.domain.coupon.entity.Coupon;
import com.kkh.shop_1.domain.coupon.entity.DiscountType;
import com.kkh.shop_1.domain.coupon.entity.UserCoupon;
import com.kkh.shop_1.domain.coupon.service.CouponService;
import com.kkh.shop_1.domain.item.entity.Item;
import com.kkh.shop_1.domain.item.entity.ItemCategory;
import com.kkh.shop_1.domain.item.entity.ItemStatus;
import com.kkh.shop_1.domain.item.service.ItemService;
import com.kkh.shop_1.domain.order.dto.OrderPaidEventPayload;
import com.kkh.shop_1.domain.order.dto.OrderRequestDTO;
import com.kkh.shop_1.domain.order.entity.Order;
import com.kkh.shop_1.domain.order.entity.OrderItem;
import com.kkh.shop_1.domain.order.entity.OrderStatus;
import com.kkh.shop_1.domain.order.repository.OrderRepository;
import com.kkh.shop_1.domain.outbox.service.OutboxEventService;
import com.kkh.shop_1.domain.user.entity.Address;
import com.kkh.shop_1.domain.user.entity.User;
import com.kkh.shop_1.domain.user.service.AddressService;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderTxHandlerTest {

    @InjectMocks
    private OrderTxHandler orderTxHandler;

    @Mock
    private AddressService addressService;

    @Mock
    private ItemService itemService;

    @Mock
    private CouponService couponService;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartItemService cartItemService;

    @Mock
    private OutboxEventService outboxEventService;

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

    private Order buildOrder(Long orderId, int quantity) {
        Order order = Order.create(user, address);
        ReflectionTestUtils.setField(order, "id", orderId);
        order.addOrderItem(OrderItem.create(item, quantity));
        return order;
    }

    @Nested
    @DisplayName("getOrCreateAddress()")
    class GetOrCreateAddressTest {

        @Test
        @DisplayName("addressId가 있으면 기존 배송지를 조회해서 재사용한다")
        void addressIdProvided_ReusesExistingAddress() {
            // given
            OrderRequestDTO dto = OrderRequestDTO.builder().addressId(1L).build();
            given(addressService.findById(1L)).willReturn(Optional.of(address));

            // when
            Address result = orderTxHandler.getOrCreateAddress(user, dto);

            // then
            assertThat(result).isEqualTo(address);
            verify(addressService, never()).save(any(Address.class));
        }

        @Test
        @DisplayName("addressId가 없으면 새 배송지를 생성하고 저장한다")
        void addressIdMissing_CreatesAndSavesNewAddress() {
            // given
            OrderRequestDTO dto = OrderRequestDTO.builder()
                    .zipCode("54321")
                    .roadAddress("New Road")
                    .detailAddress("202")
                    .recipientName("New Recipient")
                    .recipientPhone("010-9876-5432")
                    .build();

            // when
            Address result = orderTxHandler.getOrCreateAddress(user, dto);

            // then
            assertThat(result.getZipCode()).isEqualTo("54321");
            assertThat(result.getRoadAddress()).isEqualTo("New Road");
            assertThat(user.getAddresses()).contains(result);
            verify(addressService, never()).findById(anyLong());
            verify(addressService).save(result);
        }

        @Test
        @DisplayName("addressId가 있지만 존재하지 않으면 예외를 던진다")
        void addressIdProvided_NotFound_ThrowsException() {
            // given
            OrderRequestDTO dto = OrderRequestDTO.builder().addressId(999L).build();
            given(addressService.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> orderTxHandler.getOrCreateAddress(user, dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("배송지 정보를 찾을 수 없습니다.");
        }
    }

    @Nested
    @DisplayName("createOrder()")
    class CreateOrderTest {

        @Test
        @DisplayName("주문 상품마다 재고를 차감하고 주문을 저장한다")
        void createsOrder_DecreasesStockForEachItem() {
            // given
            OrderRequestDTO.ItemOrder itemOrder = OrderRequestDTO.ItemOrder.builder()
                    .itemId(1L)
                    .quantity(3)
                    .build();
            OrderRequestDTO dto = OrderRequestDTO.builder()
                    .itemOrders(Collections.singletonList(itemOrder))
                    .build();

            given(itemService.findById(1L)).willReturn(Optional.of(item));
            given(orderRepository.save(any(Order.class))).willAnswer(invocation -> {
                Order savedOrder = invocation.getArgument(0);
                ReflectionTestUtils.setField(savedOrder, "id", 100L);
                return savedOrder;
            });

            // when
            Order result = orderTxHandler.createOrder(user, address, dto);

            // then
            assertThat(result.getId()).isEqualTo(100L);
            assertThat(result.getOrderItems()).hasSize(1);
            assertThat(result.getOrderItems().get(0).getQuantity()).isEqualTo(3);
            verify(itemService).decreaseStock(1L, 3);
            verify(orderRepository).save(any(Order.class));
        }

        @Test
        @DisplayName("상품을 찾을 수 없으면 예외가 전파된다")
        void itemNotFound_PropagatesException() {
            // given
            OrderRequestDTO.ItemOrder itemOrder = OrderRequestDTO.ItemOrder.builder()
                    .itemId(99L)
                    .quantity(1)
                    .build();
            OrderRequestDTO dto = OrderRequestDTO.builder()
                    .itemOrders(Collections.singletonList(itemOrder))
                    .build();

            given(itemService.findById(99L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> orderTxHandler.createOrder(user, address, dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("상품 없음");

            verify(orderRepository, never()).save(any(Order.class));
        }

        @Test
        @DisplayName("쿠폰이 지정되어 있으면 보유 쿠폰을 조회하여 주문 상품에 적용한다")
        void couponIdProvided_AppliesUsableUserCoupon() {
            // given
            Coupon coupon = Coupon.builder()
                    .id(1L)
                    .discountType(DiscountType.FIXED)
                    .discountValue(2000)
                    .totalQuantity(10)
                    .issuedQuantity(1)
                    .build();
            UserCoupon userCoupon = UserCoupon.builder()
                    .id(10L)
                    .user(user)
                    .coupon(coupon)
                    .used(false)
                    .build();

            OrderRequestDTO.ItemOrder itemOrder = OrderRequestDTO.ItemOrder.builder()
                    .itemId(1L)
                    .quantity(1)
                    .couponId(1L)
                    .build();
            OrderRequestDTO dto = OrderRequestDTO.builder()
                    .itemOrders(Collections.singletonList(itemOrder))
                    .build();

            given(itemService.findById(1L)).willReturn(Optional.of(item));
            given(couponService.getUsableUserCoupon(user.getId(), 1L)).willReturn(userCoupon);
            given(orderRepository.save(any(Order.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            Order result = orderTxHandler.createOrder(user, address, dto);

            // then
            OrderItem createdItem = result.getOrderItems().get(0);
            assertThat(createdItem.getUserCouponId()).isEqualTo(10L);
            assertThat(createdItem.getCouponDiscount()).isEqualTo(2000);
            assertThat(createdItem.getFinalPrice()).isEqualTo(8000);
        }
    }

    @Nested
    @DisplayName("updateTid()")
    class UpdateTidTest {

        @Test
        @DisplayName("주문의 tid를 갱신한다")
        void updatesOrderTid() {
            // given
            Order order = buildOrder(1L, 1);
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));

            // when
            orderTxHandler.updateTid(1L, "new-tid");

            // then
            assertThat(order.getTid()).isEqualTo("new-tid");
        }

        @Test
        @DisplayName("주문이 존재하지 않으면 예외를 던진다")
        void orderNotFound_ThrowsException() {
            // given
            given(orderRepository.findById(1L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> orderTxHandler.updateTid(1L, "new-tid"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("completeOrderPayment()")
    class CompleteOrderPaymentTest {

        @Test
        @DisplayName("결제 완료 처리 후 ORDER_PAID outbox 이벤트를 기록한다")
        void recordsOrderPaidOutboxEvent() {
            // given
            Long userId = user.getId();
            Order order = buildOrder(1L, 2); // totalAmount = 20000
            given(orderRepository.findByIdWithFetch(1L)).willReturn(Optional.of(order));

            // when
            orderTxHandler.completeOrderPayment(1L, userId);

            // then
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
            verify(orderRepository).saveAndFlush(order);

            ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
            verify(outboxEventService).record(eq("ORDER_PAID"), payloadCaptor.capture());
            OrderPaidEventPayload payload = (OrderPaidEventPayload) payloadCaptor.getValue();
            assertThat(payload.getOrderId()).isEqualTo(order.getId());
            assertThat(payload.getUserId()).isEqualTo(userId);
            assertThat(payload.getTotalAmount()).isEqualTo(order.getTotalAmount());
            assertThat(payload.getPaidAt()).isNotNull();
        }

        @Test
        @DisplayName("사용된 쿠폰이 있는 주문 상품은 쿠폰을 사용 처리한다")
        void marksAppliedUserCouponAsUsed() {
            // given
            Long userId = user.getId();
            Order order = buildOrder(1L, 1);
            Coupon coupon = Coupon.builder().id(1L).discountType(DiscountType.FIXED).discountValue(1000).build();
            order.getOrderItems().get(0).applyCoupon(coupon, 55L);

            given(orderRepository.findByIdWithFetch(1L)).willReturn(Optional.of(order));

            // when
            orderTxHandler.completeOrderPayment(1L, userId);

            // then
            verify(couponService).markUserCouponUsed(55L);
        }

        @Test
        @DisplayName("쿠폰이 적용되지 않은 주문 상품은 쿠폰 사용 처리를 호출하지 않는다")
        void noCouponApplied_DoesNotMarkCouponUsed() {
            // given
            Long userId = user.getId();
            Order order = buildOrder(1L, 1);
            given(orderRepository.findByIdWithFetch(1L)).willReturn(Optional.of(order));

            // when
            orderTxHandler.completeOrderPayment(1L, userId);

            // then
            verify(couponService, never()).markUserCouponUsed(anyLong());
        }

        @Test
        @DisplayName("결제 완료된 주문 상품의 장바구니 항목을 정리한다")
        void cleansUpCartItemsForOrderedItems() {
            // given
            Long userId = user.getId();
            Order order = buildOrder(1L, 1);
            given(orderRepository.findByIdWithFetch(1L)).willReturn(Optional.of(order));

            // when
            orderTxHandler.completeOrderPayment(1L, userId);

            // then
            verify(cartItemService).deleteCartItemsByUserIdAndItemIds(eq(userId), eq(List.of(item.getId())));
        }

        @Test
        @DisplayName("장바구니 정리 중 예외가 발생해도 삼켜지고 결제 완료 처리 자체는 성공한다")
        void cartCleanupFails_ExceptionIsSwallowed() {
            // given
            Long userId = user.getId();
            Order order = buildOrder(1L, 1);
            given(orderRepository.findByIdWithFetch(1L)).willReturn(Optional.of(order));
            willThrow(new RuntimeException("장바구니 삭제 실패"))
                    .given(cartItemService).deleteCartItemsByUserIdAndItemIds(anyLong(), anyList());

            // when & then
            assertThatCode(() -> orderTxHandler.completeOrderPayment(1L, userId))
                    .doesNotThrowAnyException();

            assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
            verify(outboxEventService).record(eq("ORDER_PAID"), any());
        }
    }

    @Nested
    @DisplayName("cancelOrderPayment()")
    class CancelOrderPaymentTest {

        @Test
        @DisplayName("주문을 취소 처리하고 주문 상품들의 재고를 복구한다")
        void cancelsOrderAndRestoresStock() {
            // given
            Item item2 = Item.builder()
                    .id(2L)
                    .name("Item2")
                    .price(5000)
                    .quantity(50)
                    .itemCategory(ItemCategory.OTHERS)
                    .status(ItemStatus.SELLING)
                    .build();

            Order order = Order.create(user, address);
            ReflectionTestUtils.setField(order, "id", 1L);
            order.addOrderItem(OrderItem.create(item, 3));
            order.addOrderItem(OrderItem.create(item2, 2));

            given(orderRepository.findByIdWithFetch(1L)).willReturn(Optional.of(order));

            // when
            orderTxHandler.cancelOrderPayment(1L);

            // then
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            verify(itemService).increaseStock(1L, 3);
            verify(itemService).increaseStock(2L, 2);
            verify(orderRepository).save(order);
        }

        @Test
        @DisplayName("주문이 존재하지 않으면 예외를 던진다")
        void orderNotFound_ThrowsException() {
            // given
            given(orderRepository.findByIdWithFetch(1L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> orderTxHandler.cancelOrderPayment(1L))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(itemService, never()).increaseStock(anyLong(), anyInt());
        }
    }
}
