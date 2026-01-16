package com.kkh.shop_1.domain.order.controller;

import com.kkh.shop_1.common.ApiResponse;
import com.kkh.shop_1.domain.order.dto.OrderDetailDTO;
import com.kkh.shop_1.domain.order.dto.OrderRequestDTO;
import com.kkh.shop_1.domain.order.dto.OrderResponseDTO;
import com.kkh.shop_1.domain.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     *
     * 상품 주문 생성
     *
     */
    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponseDTO>> order(
            @RequestBody OrderRequestDTO dto,
            @AuthenticationPrincipal Long userId
    ) {
        log.info("Order request received: userId={}, totalAmount={}", userId, dto.getTotalAmount());
        OrderResponseDTO response = orderService.orderItems(userId, dto);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     *
     * 주문 내역 단건 조회
     *
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderDetailDTO>> getOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal Long userId
    ) {
        OrderDetailDTO orderDetail = orderService.getOrder(userId, orderId);
        return ResponseEntity.ok(ApiResponse.success(orderDetail));
    }

    /**
     *
     * 내 주문 목록 전체 조회
     *
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderDetailDTO>>> getOrders(@AuthenticationPrincipal Long userId) {
        List<OrderDetailDTO> orders = orderService.getOrders(userId);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }
}