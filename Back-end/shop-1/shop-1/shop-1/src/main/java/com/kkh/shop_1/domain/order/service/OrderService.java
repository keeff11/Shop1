package com.kkh.shop_1.domain.order.service;

import com.kkh.shop_1.domain.cart.service.CartItemService;
import com.kkh.shop_1.domain.coupon.entity.Coupon;
import com.kkh.shop_1.domain.coupon.service.CouponService;
import com.kkh.shop_1.domain.item.entity.Item;
import com.kkh.shop_1.domain.item.service.ItemService;
import com.kkh.shop_1.domain.order.dto.*;
import com.kkh.shop_1.domain.order.entity.Order;
import com.kkh.shop_1.domain.order.entity.OrderItem;
import com.kkh.shop_1.domain.order.repository.OrderRepository;
import com.kkh.shop_1.domain.user.entity.Address;
import com.kkh.shop_1.domain.user.entity.User;
import com.kkh.shop_1.domain.user.service.AddressService;
import com.kkh.shop_1.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final UserService userService;
    private final ItemService itemService;
    private final CouponService couponService;
    private final OrderRepository orderRepository;
    private final AddressService addressService;
    private final PaymentServiceFactory paymentServiceFactory;
    private final CartItemService cartItemService;

    /**
     * 신규 주문 생성 및 PG사 결제 준비
     */
    public OrderResponseDTO orderItems(Long userId, OrderRequestDTO dto) {
        User user = userService.findById(userId);
        Address address = getOrCreateAddress(user, dto);

        // 1. 주문 생성 (이 과정에서 재고 차감 수행)
        Order order = createOrder(user, address, dto.getItemOrders());
        order.setPaymentType(dto.getPaymentType());
        orderRepository.save(order);

        // 2. 결제 준비 요청 (PG사 통신)
        // 참고: 트랜잭션 내외부 분리는 구조 변경이 크므로 유지하되, DB 락 시간을 최소화하는 방향으로 개선
        PaymentReadyResponseDTO paymentResponse = preparePayment(user, order, dto);

        // 3. TID 업데이트
        order.updateTid(paymentResponse.getTid());

        return OrderResponseDTO.of(order, address, paymentResponse);
    }

    /**
     * 결제 승인 완료 처리 및 장바구니 비우기
     */

    public OrderDetailDTO approveOrder(OrderApproveDTO dto, Long userId) {
        // 1. "ORDER_" 접두사 제거 및 숫자 변환 로직 추가
        Long realOrderId;
        try {
            String rawId = dto.getOrderId();
            if (rawId.startsWith("ORDER_")) {
                realOrderId = Long.parseLong(rawId.replace("ORDER_", ""));
            } else {
                realOrderId = Long.parseLong(rawId);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("잘못된 주문 ID 형식입니다: " + dto.getOrderId());
        }

        // 2. 변환된 realOrderId로 주문 조회
        Order order = fetchOrder(realOrderId);

        // 3. 본인 주문 검증
        validateOrderOwner(order, userId);

        // 4. PG사 승인 요청
        PaymentApproveRequestDTO approveRequest = PaymentApproveRequestDTO.builder()
                .paymentType(order.getPaymentType())
                // 토스 결제 시엔 DB의 TID가 "ORDER_..." 형식이므로 그대로 사용
                .tid(order.getTid())
                .partnerOrderId(order.getId().toString())
                .partnerUserId(userId.toString())
                .pgToken(dto.getPg_token())
                .paymentKey(dto.getPaymentKey())
                .amount(dto.getAmount())
                .build();

        paymentServiceFactory.getService(order.getPaymentType()).approve(approveRequest);

        // 5. 주문 상태 변경 및 저장
        order.completePayment(order.getTid());
        orderRepository.saveAndFlush(order);

        // 6. 장바구니 비우기 (기존 로직 유지)
        try {
            List<Long> orderedItemIds = order.getOrderItems().stream()
                    .map(orderItem -> orderItem.getItem().getId())
                    .toList();

            if (!orderedItemIds.isEmpty()) {
                cartItemService.deleteCartItemsByUserIdAndItemIds(userId, orderedItemIds);
            }
        } catch (Exception e) {
            log.error("장바구니 삭제 실패", e);
        }

        return OrderDetailDTO.from(order);
    }

    public Optional<OrderItem> findByOrderItemId(Long orderItemId) {
        return orderRepository.findOrderItemById(orderItemId);
    }

    @Transactional(readOnly = true)
    public OrderDetailDTO getOrder(Long userId, Long orderId) {
        Order order = fetchOrder(orderId);
        validateOrderOwner(order, userId);
        return OrderDetailDTO.from(order);
    }

    @Transactional(readOnly = true)
    public List<OrderDetailDTO> getOrders(Long userId) {
        User user = userService.findById(userId);
        // [핵심 수정] N+1 문제 해결을 위한 Repository 메서드 호출
        return orderRepository.findByUserWithFetch(user).stream()
                .map(OrderDetailDTO::from)
                .toList();
    }

    private Address getOrCreateAddress(User user, OrderRequestDTO dto) {
        if (dto.getAddressId() != null) {
            return addressService.findById(dto.getAddressId())
                    .orElseThrow(() -> new IllegalArgumentException("배송지 정보를 찾을 수 없습니다."));
        }
        Address newAddress = Address.create(
                user,
                dto.getZipCode(),
                dto.getRoadAddress(),
                dto.getDetailAddress(),
                dto.getRecipientName(),
                dto.getRecipientPhone()
        );
        addressService.save(newAddress);
        user.addAddress(newAddress);
        return newAddress;
    }

    private Order createOrder(User user, Address address, List<OrderRequestDTO.ItemOrder> itemOrders) {
        Order order = Order.create(user, address);

        for (OrderRequestDTO.ItemOrder io : itemOrders) {
            // [핵심 수정] 1. 동시성 제어를 위해 ItemService를 통해 DB Atomic Update 수행
            itemService.decreaseStock(io.getItemId(), io.getQuantity());

            // 2. 주문 생성을 위해 엔티티 조회 (재고는 이미 차감됨)
            Item item = itemService.findById(io.getItemId())
                    .orElseThrow(() -> new IllegalArgumentException("상품 없음 ID: " + io.getItemId()));

            OrderItem orderItem = OrderItem.create(item, io.getQuantity());

            if (io.getCouponId() != null) {
                Coupon coupon = couponService.getCouponById(io.getCouponId());
                orderItem.applyCoupon(coupon);
            }
            order.addOrderItem(orderItem);
        }
        return order;
    }

    private PaymentReadyResponseDTO preparePayment(User user, Order order, OrderRequestDTO dto) {
        PaymentService paymentService = paymentServiceFactory.getService(dto.getPaymentType());
        String approvalUrl = dto.getApprovalUrl().replace("{orderId}", order.getId().toString());
        return paymentService.ready(PaymentReadyRequestDTO.of(user, order, dto, approvalUrl));
    }

    private Order fetchOrder(Long orderId) {
        return orderRepository.findByIdWithFetch(orderId)
                .orElseThrow(() -> new IllegalArgumentException("해당 주문이 존재하지 않습니다. ID: " + orderId));
    }

    private void validateOrderOwner(Order order, Long userId) {
        if (!order.getUser().getId().equals(userId)) {
            throw new SecurityException("해당 주문에 대한 열람 권한이 없습니다.");
        }
    }
}