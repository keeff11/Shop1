package com.kkh.shop_1.domain.order.service;

import com.kkh.shop_1.common.annotation.Idempotent;
import com.kkh.shop_1.domain.order.dto.*;
import com.kkh.shop_1.domain.order.entity.Order;
import com.kkh.shop_1.domain.order.entity.OrderItem;
import com.kkh.shop_1.domain.order.entity.OrderStatus;
import com.kkh.shop_1.domain.order.repository.OrderRepository;
import com.kkh.shop_1.domain.user.entity.Address;
import com.kkh.shop_1.domain.user.entity.User;
import com.kkh.shop_1.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final UserService userService;
    private final OrderRepository orderRepository;
    private final PaymentServiceFactory paymentServiceFactory;
    private final OrderTxHandler orderTxHandler;

    private final RedissonClient redissonClient;

    public OrderResponseDTO orderItems(Long userId, OrderRequestDTO dto) {
        
        User user = userService.findById(userId);

        List<Long> sortedItemIds = dto.getItemOrders().stream()
                .map(OrderRequestDTO.ItemOrder::getItemId)
                .distinct()
                .sorted()
                .toList();

        List<RLock> locks = sortedItemIds.stream()
                .map(id -> redissonClient.getLock("ITEM_LOCK:" + id))
                .toList();
        RLock multiLock = redissonClient.getMultiLock(locks.toArray(new RLock[0]));

        Address address;
        Order order;
        boolean isLocked = false;

        try {
            isLocked = multiLock.tryLock(10, 5, TimeUnit.SECONDS);
            if (!isLocked) {
                log.warn("Redis MultiLock 획득 실패 (userId: {})", userId);
                throw new IllegalStateException("현재 요청이 많아 처리가 지연되고 있습니다. 잠시 후 다시 시도해주세요.");
            }

            address = orderTxHandler.getOrCreateAddress(user, dto);
            order = orderTxHandler.createOrder(user, address, dto);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("주문 처리 중 오류가 발생했습니다.");
        } finally {
            if (isLocked) {
                multiLock.unlock();
            }
        }

        PaymentReadyResponseDTO paymentResponse;
        try {
            paymentResponse = preparePayment(user, order, dto);
        } catch (Exception e) {
            log.error("결제 준비 실패. 주문을 취소하고 재고를 복구합니다. orderId: {}", order.getId(), e);
            orderTxHandler.cancelOrderPayment(order.getId());
            throw new IllegalStateException("결제 준비 중 오류가 발생하여 주문이 취소되었습니다.", e);
        }
        orderTxHandler.updateTid(order.getId(), paymentResponse.getTid());
        return OrderResponseDTO.of(order, address, paymentResponse);
    }

    @Idempotent(ttlHours = 24)
    public OrderDetailDTO approveOrder(OrderApproveDTO dto, Long userId) {
        Long realOrderId;
        try {
            String rawId = dto.getOrderId();
            if (rawId.startsWith("ORDER_")) {
                String[] parts = rawId.split("_");
                realOrderId = Long.parseLong(parts[1]);
            } else {
                realOrderId = Long.parseLong(rawId);
            }
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("잘못된 주문 ID 형식입니다: " + dto.getOrderId());
        }

        // 동일 주문에 대한 결제 승인 요청이 동시에 들어와도(더블클릭, PG 콜백 재시도 등)
        // 하나씩 순차 처리되도록 주문 단위 락을 건다. leaseTime을 지정하지 않아
        // PG 승인 호출이 오래 걸려도 Redisson watchdog이 락을 자동 연장한다.
        RLock approveLock = redissonClient.getLock("ORDER_APPROVE:" + realOrderId);
        boolean isLocked = false;
        try {
            isLocked = approveLock.tryLock(10, TimeUnit.SECONDS);
            if (!isLocked) {
                throw new IllegalStateException("이미 해당 주문의 결제 승인이 처리 중입니다. 잠시 후 다시 시도해주세요.");
            }
            return doApprove(realOrderId, dto, userId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("결제 승인 처리 중 오류가 발생했습니다.");
        } finally {
            if (isLocked) {
                approveLock.unlock();
            }
        }
    }

    private OrderDetailDTO doApprove(Long realOrderId, OrderApproveDTO dto, Long userId) {
        Order order = fetchOrder(realOrderId);
        validateOrderOwner(order, userId);

        if (order.getStatus() == OrderStatus.PAID) {
            log.info("이미 결제가 완료된 주문입니다.(orderId: {})", realOrderId);
            return OrderDetailDTO.from(order);
        }

        // 결제 금액은 클라이언트 값을 신뢰하지 않고 서버가 계산한 주문 총액을 기준으로 검증/사용한다.
        int totalAmount = order.getTotalAmount();
        if (dto.getAmount() != null && dto.getAmount() != totalAmount) {
            log.warn("결제 금액 불일치. orderId: {}, 요청 금액: {}, 주문 금액: {}", realOrderId, dto.getAmount(), totalAmount);
            throw new IllegalArgumentException("결제 금액이 주문 금액과 일치하지 않습니다.");
        }

        PaymentApproveRequestDTO approveRequest = PaymentApproveRequestDTO.builder()
                .paymentType(order.getPaymentType())
                .tid(order.getTid())
                .partnerOrderId(order.getId().toString())
                .partnerUserId(userId.toString())
                .pgToken(dto.getPg_token())
                .paymentKey(dto.getPaymentKey())
                .amount(totalAmount)
                .build();

        paymentServiceFactory.getService(order.getPaymentType()).approve(approveRequest);

        try {
            orderTxHandler.completeOrderPayment(order.getId(), userId);
        } catch (Exception e) {
            log.error("DB 결제 상태 업데이트 실패. 결제 취소를 진행합니다. orderId: {}", order.getId(), e);
            paymentServiceFactory.getService(order.getPaymentType())
                    .cancel(dto.getPaymentKey(), "내부 시스템 오류로 인한 자동 취소", totalAmount);
            throw new IllegalStateException("결제 처리 중 내부 오류가 발생하여 결제가 자동 취소되었습니다.");
        }

        Order updatedOrder = fetchOrder(order.getId());
        return OrderDetailDTO.from(updatedOrder);
    }

    @Transactional(readOnly = true)
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
        return orderRepository.findByUserWithFetch(user).stream()
                .map(OrderDetailDTO::from)
                .toList();
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