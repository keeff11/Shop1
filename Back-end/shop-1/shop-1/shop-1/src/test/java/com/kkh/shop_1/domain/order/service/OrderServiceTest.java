package com.kkh.shop_1.domain.order.service;

import com.kkh.shop_1.domain.cart.service.CartItemService;
import com.kkh.shop_1.domain.coupon.service.CouponService;
import com.kkh.shop_1.domain.item.entity.Item;
import com.kkh.shop_1.domain.item.service.ItemService;
import com.kkh.shop_1.domain.order.dto.*;
import com.kkh.shop_1.domain.order.entity.Order;
import com.kkh.shop_1.domain.order.entity.OrderItem;
import com.kkh.shop_1.domain.order.entity.OrderStatus;
import com.kkh.shop_1.domain.order.entity.PaymentType;
import com.kkh.shop_1.domain.order.repository.OrderRepository;
import com.kkh.shop_1.domain.user.entity.Address;
import com.kkh.shop_1.domain.user.entity.User;
import com.kkh.shop_1.domain.user.service.AddressService;
import com.kkh.shop_1.domain.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @InjectMocks
    private OrderService orderService;

    @Mock
    private UserService userService;

    @Mock
    private ItemService itemService;

    @Mock
    private CouponService couponService;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private AddressService addressService;

    @Mock
    private PaymentServiceFactory paymentServiceFactory;

    @Mock
    private PaymentService paymentService;

    @Mock
    private CartItemService cartItemService;

    private User user;
    private Item item;
    private Address address;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).nickname("testUser").build();
        item = Item.builder().id(1L).name("Test Item").price(10000).quantity(100).build(); // stock -> quantity 수정
        address = Address.builder()
                .id(1L)
                .user(user)
                .zipCode("12345")
                .roadAddress("Test Road")
                .detailAddress("101")
                .recipientName("Test Recipient")
                .recipientPhone("010-1234-5678")
                .build();
    }

    @Test
    @DisplayName("주문 및 결제 준비 - 성공 (기존 배송지 사용)")
    void orderItems_Success_WithExistingAddress() {
        // given
        Long userId = 1L;
        OrderRequestDTO.ItemOrder itemOrder = new OrderRequestDTO.ItemOrder(1L, 2, null);
        OrderRequestDTO requestDTO = OrderRequestDTO.builder()
                .addressId(1L)
                .itemOrders(Collections.singletonList(itemOrder))
                .paymentType(PaymentType.KAKAO_PAY) // String -> PaymentType 수정
                .approvalUrl("http://localhost/approve?orderId={orderId}")
                .cancelUrl("http://localhost/cancel")
                .failUrl("http://localhost/fail")
                .build();

        PaymentReadyResponseDTO paymentReadyResponse = new PaymentReadyResponseDTO("tid-123", "http://redirect.url");

        when(userService.findById(userId)).thenReturn(user);
        when(addressService.findById(1L)).thenReturn(Optional.of(address));
        when(itemService.findById(1L)).thenReturn(Optional.of(item));
        when(paymentServiceFactory.getService(PaymentType.KAKAO_PAY)).thenReturn(paymentService); // String -> PaymentType 수정
        when(paymentService.ready(any())).thenReturn(paymentReadyResponse);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            ReflectionTestUtils.setField(order, "id", 100L);
            return order;
        });

        // when
        OrderResponseDTO responseDTO = orderService.orderItems(userId, requestDTO);

        // then
        assertThat(responseDTO).isNotNull();
        assertThat(responseDTO.getTid()).isEqualTo("tid-123");
        assertThat(responseDTO.getRedirectUrl()).isEqualTo("http://redirect.url");
        assertThat(responseDTO.getAddressId()).isEqualTo(address.getId());

        verify(userService).findById(userId);
        verify(addressService).findById(1L);
        verify(addressService, never()).save(any(Address.class));
        verify(itemService).findById(1L);
        verify(orderRepository).save(any(Order.class));
        verify(paymentServiceFactory).getService(PaymentType.KAKAO_PAY); // String -> PaymentType 수정
        verify(paymentService).ready(any());
    }

    @Test
    @DisplayName("주문 및 결제 준비 - 성공 (신규 배송지 사용)")
    void orderItems_Success_WithNewAddress() {
        // given
        Long userId = 1L;
        OrderRequestDTO.ItemOrder itemOrder = new OrderRequestDTO.ItemOrder(1L, 2, null);
        OrderRequestDTO requestDTO = OrderRequestDTO.builder()
                .zipCode("54321")
                .roadAddress("New Road")
                .detailAddress("202")
                .recipientName("New Recipient")
                .recipientPhone("010-9876-5432")
                .itemOrders(Collections.singletonList(itemOrder))
                .paymentType(PaymentType.KAKAO_PAY) // String -> PaymentType 수정
                .approvalUrl("http://localhost/approve?orderId={orderId}")
                .build();

        PaymentReadyResponseDTO paymentReadyResponse = new PaymentReadyResponseDTO("tid-456", "http://redirect.url/new");

        when(userService.findById(userId)).thenReturn(user);
        when(itemService.findById(1L)).thenReturn(Optional.of(item));
        when(paymentServiceFactory.getService(PaymentType.KAKAO_PAY)).thenReturn(paymentService); // String -> PaymentType 수정
        when(paymentService.ready(any())).thenReturn(paymentReadyResponse);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            ReflectionTestUtils.setField(order, "id", 101L);
            return order;
        });

        // when
        OrderResponseDTO responseDTO = orderService.orderItems(userId, requestDTO);

        // then
        assertThat(responseDTO).isNotNull();
        assertThat(responseDTO.getTid()).isEqualTo("tid-456");
        assertThat(responseDTO.getRedirectUrl()).isEqualTo("http://redirect.url/new");
        assertThat(responseDTO.getZipCode()).isEqualTo("54321");

        verify(userService).findById(userId);
        verify(addressService, never()).findById(anyLong());
        verify(addressService).save(any(Address.class));
        verify(itemService).findById(1L);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @DisplayName("주문 및 결제 준비 - 실패 (상품 없음)")
    void orderItems_Fail_ItemNotFound() {
        // given
        Long userId = 1L;
        OrderRequestDTO.ItemOrder itemOrder = new OrderRequestDTO.ItemOrder(99L, 1, null); // Non-existent item
        OrderRequestDTO requestDTO = OrderRequestDTO.builder()
                .addressId(1L)
                .itemOrders(Collections.singletonList(itemOrder))
                .build();

        when(userService.findById(userId)).thenReturn(user);
        when(addressService.findById(1L)).thenReturn(Optional.of(address));
        when(itemService.findById(99L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> orderService.orderItems(userId, requestDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("상품 없음");
    }

    @Test
    @DisplayName("주문 단건 조회 - 성공")
    void getOrder_Success() {
        // given
        Long userId = 1L;
        Long orderId = 1L;

        Order order = Order.create(user, address);
        order.addOrderItem(OrderItem.create(item, 2));
        ReflectionTestUtils.setField(order, "id", orderId);
        ReflectionTestUtils.setField(order, "status", OrderStatus.PAID);
        ReflectionTestUtils.setField(order, "orderDate", LocalDateTime.now());

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // when
        OrderDetailDTO orderDetail = orderService.getOrder(userId, orderId);

        // then
        assertThat(orderDetail).isNotNull();
        assertThat(orderDetail.getOrderId()).isEqualTo(orderId);
        assertThat(orderDetail.getStatus()).isEqualTo(OrderStatus.PAID.name());
        assertThat(orderDetail.getItems()).hasSize(1);
        assertThat(orderDetail.getItems().get(0).getItemId()).isEqualTo(item.getId());
        assertThat(orderDetail.getTotalPrice()).isEqualTo(20000);
        assertThat(orderDetail.getAddress().getRoadAddress()).isEqualTo(address.getRoadAddress());

        verify(orderRepository).findById(orderId);
    }

    @Test
    @DisplayName("주문 단건 조회 - 실패 (권한 없음)")
    void getOrder_Fail_Unauthorized() {
        // given
        Long wrongUserId = 2L;
        Long orderId = 1L;

        Order order = Order.create(user, address); // user has id 1L
        ReflectionTestUtils.setField(order, "id", orderId);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // when & then
        assertThatThrownBy(() -> orderService.getOrder(wrongUserId, orderId))
                .isInstanceOf(SecurityException.class)
                .hasMessage("해당 주문에 대한 열람 권한이 없습니다.");
    }

    @Test
    @DisplayName("주문 목록 조회 - 성공")
    void getOrders_Success() {
        // given
        Long userId = 1L;

        Order order1 = Order.create(user, address);
        ReflectionTestUtils.setField(order1, "id", 1L);
        order1.addOrderItem(OrderItem.create(item, 1));
        ReflectionTestUtils.setField(order1, "status", OrderStatus.PAID);
        ReflectionTestUtils.setField(order1, "orderDate", LocalDateTime.now());

        Item item2 = Item.builder().id(2L).name("Item 2").price(5000).build();
        Order order2 = Order.create(user, address);
        ReflectionTestUtils.setField(order2, "id", 2L);
        order2.addOrderItem(OrderItem.create(item2, 3));
        ReflectionTestUtils.setField(order2, "status", OrderStatus.PAID);
        ReflectionTestUtils.setField(order2, "orderDate", LocalDateTime.now().minusDays(1));

        when(userService.findById(userId)).thenReturn(user);
        when(orderRepository.findByUserWithFetch(user)).thenReturn(List.of(order1, order2));

        // when
        List<OrderDetailDTO> orders = orderService.getOrders(userId);

        // then
        assertThat(orders).isNotNull();
        assertThat(orders).hasSize(2);

        OrderDetailDTO resultOrder1 = orders.stream().filter(o -> o.getOrderId().equals(1L)).findFirst().get();
        assertThat(resultOrder1.getTotalPrice()).isEqualTo(10000);
        assertThat(resultOrder1.getStatus()).isEqualTo(OrderStatus.PAID.name());

        OrderDetailDTO resultOrder2 = orders.stream().filter(o -> o.getOrderId().equals(2L)).findFirst().get();
        assertThat(resultOrder2.getTotalPrice()).isEqualTo(15000);
        assertThat(resultOrder2.getStatus()).isEqualTo(OrderStatus.PAID.name());

        verify(userService).findById(userId);
        verify(orderRepository).findByUserWithFetch(user);
    }

    @Test
    @DisplayName("주문 목록 조회 - 주문 내역 없음")
    void getOrders_Success_NoOrders() {
        // given
        Long userId = 1L;
        when(userService.findById(userId)).thenReturn(user);
        when(orderRepository.findByUserWithFetch(user)).thenReturn(Collections.emptyList());

        // when
        List<OrderDetailDTO> orders = orderService.getOrders(userId);

        // then
        assertThat(orders).isNotNull();
        assertThat(orders).isEmpty();

        verify(userService).findById(userId);
        verify(orderRepository).findByUserWithFetch(user);
    }

    @Test
    @DisplayName("결제 승인 - 성공")
    void approveOrder_Success() {
        // given
        Long userId = 1L;
        Long orderId = 1L;
        String pgToken = "pg-token";
        String tid = "tid-123";

        Order order = Order.create(user, address);
        ReflectionTestUtils.setField(order, "id", orderId);
        ReflectionTestUtils.setField(order, "tid", tid);
        ReflectionTestUtils.setField(order, "paymentType", PaymentType.KAKAO_PAY); // String -> PaymentType 수정
        ReflectionTestUtils.setField(order, "status", OrderStatus.PAID);
        ReflectionTestUtils.setField(order, "orderDate", LocalDateTime.now());
        order.addOrderItem(OrderItem.create(item, 1));

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(paymentServiceFactory.getService(PaymentType.KAKAO_PAY)).thenReturn(paymentService); // String -> PaymentType 수정

        // when
        OrderDetailDTO result = orderService.approveOrder(orderId, pgToken, userId);

        // then
        assertThat(result.getStatus()).isEqualTo(OrderStatus.PAID.name());
        verify(paymentService).approve(any(PaymentApproveRequestDTO.class));
        verify(orderRepository).saveAndFlush(order);
        verify(cartItemService).deleteCartItemsByUserIdAndItemIds(eq(userId), anyList());
    }
}
