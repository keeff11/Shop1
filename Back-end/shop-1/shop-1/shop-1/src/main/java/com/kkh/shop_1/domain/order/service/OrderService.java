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
    public OrderDetailDTO approveOrder(Long orderId, String pgToken, Long userId) {
        Order order = fetchOrder(orderId);

        // 1. 본인 주문 검증
        validateOrderOwner(order, userId);

        // 2. PG사 승인 요청
        PaymentApproveRequestDTO approveRequest = PaymentApproveRequestDTO.builder()
                .paymentType(order.getPaymentType())
                .tid(order.getTid())
                .partnerOrderId(order.getId().toString())
                .partnerUserId(userId.toString())
                .pgToken(pgToken)
                .build();

        paymentServiceFactory.getService(order.getPaymentType()).approve(approveRequest);

        // 3. 주문 상태 변경 (PAID)
        order.completePayment(order.getTid());

        // [핵심 수정] 상태 변경 사항 즉시 반영 (Flush)
        orderRepository.saveAndFlush(order);

        // 4. DTO 변환
        OrderDetailDTO responseDTO = OrderDetailDTO.from(order);

        // 5. 장바구니 비우기 (오류 발생 시 로그만 남기고 주문 로직은 정상 완료)
        try {
            List<Long> orderedItemIds = order.getOrderItems().stream()
                    .map(orderItem -> orderItem.getItem().getId())
                    .toList();

            if (!orderedItemIds.isEmpty()) {
                cartItemService.deleteCartItemsByUserIdAndItemIds(userId, orderedItemIds);
                log.info("장바구니 상품 삭제 완료 - UserID: {}, ItemIDs: {}", userId, orderedItemIds);
            }
        } catch (Exception e) {
            log.error("장바구니 삭제 중 오류 발생 (주문은 성공): {}", e.getMessage());
        }

        log.info("주문 결제 승인 완료 - 주문ID: {}, TID: {}", orderId, order.getTid());

        return responseDTO;
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
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("해당 주문이 존재하지 않습니다. ID: " + orderId));
    }

    private void validateOrderOwner(Order order, Long userId) {
        if (!order.getUser().getId().equals(userId)) {
            throw new SecurityException("해당 주문에 대한 열람 권한이 없습니다.");
        }
    }
}