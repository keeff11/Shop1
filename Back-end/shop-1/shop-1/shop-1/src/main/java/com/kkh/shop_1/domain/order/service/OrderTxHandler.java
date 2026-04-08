package com.kkh.shop_1.domain.order.service;

import com.kkh.shop_1.domain.cart.service.CartItemService;
import com.kkh.shop_1.domain.coupon.entity.Coupon;
import com.kkh.shop_1.domain.coupon.service.CouponService;
import com.kkh.shop_1.domain.item.entity.Item;
import com.kkh.shop_1.domain.item.service.ItemService;
import com.kkh.shop_1.domain.order.dto.OrderRequestDTO;
import com.kkh.shop_1.domain.order.entity.Order;
import com.kkh.shop_1.domain.order.entity.OrderItem;
import com.kkh.shop_1.domain.order.repository.OrderRepository;
import com.kkh.shop_1.domain.user.entity.Address;
import com.kkh.shop_1.domain.user.entity.User;
import com.kkh.shop_1.domain.user.service.AddressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTxHandler {

    private final AddressService addressService;
    private final ItemService itemService;
    private final CouponService couponService;
    private final OrderRepository orderRepository;
    private final CartItemService cartItemService;

    @Transactional
    public Address getOrCreateAddress(User user, OrderRequestDTO dto) {
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

    @Transactional
    public Order createOrder(User user, Address address, OrderRequestDTO dto) {
        Order order = Order.create(user, address);
        order.setPaymentType(dto.getPaymentType());

        for (OrderRequestDTO.ItemOrder io : dto.getItemOrders()) {
            itemService.decreaseStock(io.getItemId(), io.getQuantity());

            Item item = itemService.findById(io.getItemId())
                    .orElseThrow(() -> new IllegalArgumentException("상품 없음 ID: " + io.getItemId()));

            OrderItem orderItem = OrderItem.create(item, io.getQuantity());

            if (io.getCouponId() != null) {
                Coupon coupon = couponService.getCouponById(io.getCouponId());
                orderItem.applyCoupon(coupon);
            }
            order.addOrderItem(orderItem);
        }
        return orderRepository.save(order);
    }

    @Transactional
    public void updateTid(Long orderId, String tid) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("해당 주문이 존재하지 않습니다. ID: " + orderId));
        order.updateTid(tid);
    }

    @Transactional
    public void completeOrderPayment(Long orderId, Long userId) {
        Order order = orderRepository.findByIdWithFetch(orderId)
                .orElseThrow(() -> new IllegalArgumentException("해당 주문이 존재하지 않습니다. ID: " + orderId));

        order.completePayment(order.getTid());
        orderRepository.saveAndFlush(order);

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
    }

    @Transactional
    public void cancelOrderPayment(Long orderId) {
        Order order = orderRepository.findByIdWithFetch(orderId)
                .orElseThrow(() -> new IllegalArgumentException("해당 주문이 존재하지 않습니다. ID: " + orderId));

        order.failPayment();

        for (OrderItem orderItem : order.getOrderItems()) {
            itemService.increaseStock(orderItem.getItem().getId(), orderItem.getQuantity());
        }

        orderRepository.save(order);
        log.info("▶ [내부 DB 취소 처리] 주문 번호 {}에 대한 상태 변경 및 재고 복구 완료", orderId);
    }
}