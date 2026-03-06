package com.kkh.shop_1.domain.order.service;

import com.kkh.shop_1.domain.order.dto.*;
import com.kkh.shop_1.domain.order.entity.Order;
import com.kkh.shop_1.domain.order.entity.OrderItem;
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

    // 🌟 분산락 직접 제어를 위해 RedissonClient 주입
    private final RedissonClient redissonClient;

    /**
     * 신규 주문 생성 및 PG사 결제 준비
     */
    public OrderResponseDTO orderItems(Long userId, OrderRequestDTO dto) {
        User user = userService.findById(userId);

        // 1. 🌟 [핵심] 여러 상품 주문 시 교착 상태(Deadlock) 방지를 위해 상품 ID를 중복 제거 및 오름차순 정렬
        List<Long> sortedItemIds = dto.getItemOrders().stream()
                .map(OrderRequestDTO.ItemOrder::getItemId)
                .distinct()
                .sorted()
                .toList();

        // 2. 여러 상품에 대한 RLock 객체들을 모아 MultiLock 생성
        List<RLock> locks = sortedItemIds.stream()
                .map(id -> redissonClient.getLock("ITEM_LOCK:" + id))
                .toList();
        RLock multiLock = redissonClient.getMultiLock(locks.toArray(new RLock[0]));

        Address address;
        Order order;
        boolean isLocked = false;

        try {
            // 3. 🌟 DB 커넥션을 맺기 전에 Redis 분산락부터 획득 시도 (대기 10초, 점유 5초)
            isLocked = multiLock.tryLock(10, 5, TimeUnit.SECONDS);
            if (!isLocked) {
                log.warn("Redis MultiLock 획득 실패 (userId: {})", userId);
                throw new IllegalStateException("현재 요청이 많아 처리가 지연되고 있습니다. 잠시 후 다시 시도해주세요.");
            }

            // 4. 락을 획득한 1명만 DB 트랜잭션 진입 (커넥션 점유 시간을 극단적으로 최소화)
            address = orderTxHandler.getOrCreateAddress(user, dto);
            order = orderTxHandler.createOrder(user, address, dto);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("주문 처리 중 오류가 발생했습니다.");
        } finally {
            // 5. DB 트랜잭션 종료 직후 바로 락 반납 (다음 대기자 즉시 진입)
            if (isLocked) {
                multiLock.unlock();
            }
        }

        // 6. 🌟 트랜잭션과 락이 모두 해제된 자유로운 상태에서 PG사 네트워크 호출 (JMeter 병목 원천 차단)
        PaymentReadyResponseDTO paymentResponse = preparePayment(user, order, dto);

        // 7. 결제 고유번호(TID)만 별도의 짧은 트랜잭션으로 저장
        orderTxHandler.updateTid(order.getId(), paymentResponse.getTid());

        return OrderResponseDTO.of(order, address, paymentResponse);
    }

    /**
     * 결제 승인 완료 처리
     */
    public OrderDetailDTO approveOrder(OrderApproveDTO dto, Long userId) {
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

        Order order = fetchOrder(realOrderId);
        validateOrderOwner(order, userId);

        PaymentApproveRequestDTO approveRequest = PaymentApproveRequestDTO.builder()
                .paymentType(order.getPaymentType())
                .tid(order.getTid())
                .partnerOrderId(order.getId().toString())
                .partnerUserId(userId.toString())
                .pgToken(dto.getPg_token())
                .paymentKey(dto.getPaymentKey())
                .amount(dto.getAmount())
                .build();

        paymentServiceFactory.getService(order.getPaymentType()).approve(approveRequest);

        orderTxHandler.completeOrderPayment(order.getId(), userId);
        order.completePayment(order.getTid());

        return OrderDetailDTO.from(order);
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