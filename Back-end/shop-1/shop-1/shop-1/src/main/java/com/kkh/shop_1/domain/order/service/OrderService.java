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
     * 신규 주문을 생성하고 PG사 결제 준비 단계를 진행
     */
    public OrderResponseDTO orderItems(Long userId, OrderRequestDTO dto) {
        User user = userService.findById(userId);
        Address address = getOrCreateAddress(user, dto);

        // 1. 주문 생성
        Order order = createOrder(user, address, dto.getItemOrders());
        // 결제 타입 저장 (승인 시 Kakao인지 Naver인지 구분하기 위함)
        order.setPaymentType(dto.getPaymentType());
        orderRepository.save(order);

        // 2. 결제 준비 요청 (PG사 통신)
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

        // ★ [핵심 수정] 상태 변경 사항을 DB에 즉시 반영 (Flush)
        // 이후 장바구니 삭제 로직에서 영속성 컨텍스트가 초기화되더라도, 이미 DB에는 반영되었으므로 안전함.
        orderRepository.saveAndFlush(order);

        // 4. DTO 변환 (영속성 컨텍스트가 살아있을 때 수행)
        OrderDetailDTO responseDTO = OrderDetailDTO.from(order);

        // ==========================================
        // 장바구니 비우기 로직 (Bulk Delete)
        // ==========================================
        try {
            List<Long> orderedItemIds = order.getOrderItems().stream()
                    .map(orderItem -> orderItem.getItem().getId())
                    .toList();

            if (!orderedItemIds.isEmpty()) {
                // @Modifying(clearAutomatically = true) 때문에 여기서 영속성 컨텍스트가 비워짐
                cartItemService.deleteCartItemsByUserIdAndItemIds(userId, orderedItemIds);
                log.info("장바구니 상품 삭제 완료 - UserID: {}, ItemIDs: {}", userId, orderedItemIds);
            }
        } catch (Exception e) {
            log.error("장바구니 삭제 중 오류 발생: {}", e.getMessage());
        }
        // ==========================================

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
        return orderRepository.findByUser(user).stream()
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
            Item item = itemService.findById(io.getItemId())
                    .orElseThrow(() -> new IllegalArgumentException("상품 없음 ID: " + io.getItemId()));

            item.updateStock(item.getQuantity() - io.getQuantity());
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