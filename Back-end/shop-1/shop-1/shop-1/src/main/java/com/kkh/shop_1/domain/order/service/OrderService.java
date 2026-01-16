package com.kkh.shop_1.domain.order.service;

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

    /**
     *
     * 신규 주문을 생성하고 PG사 결제 준비 단계를 진행
     *
     */
    public OrderResponseDTO orderItems(Long userId, OrderRequestDTO dto) {
        User user = userService.findById(userId);
        Address address = getOrCreateAddress(user, dto);

        Order order = createOrder(user, address, dto.getItemOrders());
        orderRepository.save(order);

        PaymentReadyResponseDTO paymentResponse = preparePayment(user, order, dto);

        order.updateTid(paymentResponse.getTid());

        return OrderResponseDTO.of(order, address, paymentResponse);
    }

    /**
     *
     * 주문 ID를 통한 단건 상세 정보 조회
     *
     */
    @Transactional(readOnly = true)
    public OrderDetailDTO getOrder(Long userId, Long orderId) {
        Order order = fetchOrder(orderId);
        validateOrderOwner(order, userId);

        return OrderDetailDTO.from(order);
    }

    /**
     *
     * 특정 사용자의 전체 주문 내역 목록 조회
     *
     */
    @Transactional(readOnly = true)
    public List<OrderDetailDTO> getOrders(Long userId) {
        User user = userService.findById(userId);
        return orderRepository.findByUser(user).stream()
                .map(OrderDetailDTO::from)
                .toList();
    }

    /**
     *
     * 요청 데이터에 기반하여 기존 배송지를 조회하거나 신규 배송지를 생성
     *
     */
    private Address getOrCreateAddress(User user, OrderRequestDTO dto) {
        if (dto.getAddressId() != null) {
            return addressService.findById(dto.getAddressId())
                    .orElseThrow(() -> new IllegalArgumentException("배송지 정보를 찾을 수 없습니다."));
        }

        // [수정 포인트] Address.create()의 파라미터 규격에 맞게 DTO 필드 전달
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

    /**
     *
     * 주문 상품별 재고 차감 및 주문 엔티티 조립
     *
     */
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

    /**
     *
     * 결제 수단에 맞는 서비스를 호출하여 결제 준비(Ready) 수행
     *
     */
    private PaymentReadyResponseDTO preparePayment(User user, Order order, OrderRequestDTO dto) {
        PaymentService paymentService = paymentServiceFactory.getService(dto.getPaymentType());

        String approvalUrl = dto.getApprovalUrl().replace("{orderId}", order.getId().toString());

        return paymentService.ready(PaymentReadyRequestDTO.of(user, order, dto, approvalUrl));
    }

    /**
     *
     * 주문 엔티티 존재 여부 확인 및 반환
     *
     */
    private Order fetchOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("해당 주문이 존재하지 않습니다. ID: " + orderId));
    }

    /**
     *
     * 조회 요청자와 주문 소유자 일치 여부 검증
     *
     */
    private void validateOrderOwner(Order order, Long userId) {
        if (!order.getUser().getId().equals(userId)) {
            throw new SecurityException("해당 주문에 대한 열람 권한이 없습니다.");
        }
    }
}