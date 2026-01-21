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
     * [주문 생성]
     */
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> order(
            @RequestBody OrderRequestDTO dto,
            @AuthenticationPrincipal Long userId
    ) {
        OrderResponseDTO response = orderService.orderItems(userId, dto);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * [결제 승인]
     */
    @GetMapping("/payment/approve")
    public ResponseEntity<ApiResponse<OrderDetailDTO>> approveOrder(
            @RequestParam("orderId") Long orderId,
            @RequestParam("pg_token") String pgToken,
            @AuthenticationPrincipal Long userId
    ) {
        // 서비스 메서드 호출 시 인자 3개 전달
        OrderDetailDTO response = orderService.approveOrder(orderId, pgToken, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * [단건 상세 조회]
     */
    @GetMapping("/detail/{orderId}")
    public ResponseEntity<ApiResponse<OrderDetailDTO>> getOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal Long userId
    ) {
        OrderDetailDTO orderDetail = orderService.getOrder(userId, orderId);
        return ResponseEntity.ok(ApiResponse.success(orderDetail));
    }

    /**
     * [목록 조회]
     */
    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<OrderDetailDTO>>> getOrders(@AuthenticationPrincipal Long userId) {
        List<OrderDetailDTO> orders = orderService.getOrders(userId);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }
}