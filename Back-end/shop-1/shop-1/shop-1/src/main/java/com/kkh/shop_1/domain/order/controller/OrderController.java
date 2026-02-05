package com.kkh.shop_1.domain.order.controller;

import com.kkh.shop_1.common.ApiResponse;
import com.kkh.shop_1.domain.order.dto.OrderApproveDTO;
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
     * 결제 승인 요청 (DTO 사용 버전)
     */
    @GetMapping("/payment/approve")
    public ApiResponse<OrderDetailDTO> approve(
            @ModelAttribute OrderApproveDTO request,
            @AuthenticationPrincipal Long userId
    ) {
        // Service 메서드 호출 시 DTO의 값을 꺼내서 전달
        return ApiResponse.success(orderService.approveOrder(
                request,
                userId
        ));
    }

    /**
     * [단건 상세 조회]

     */
    @GetMapping("/detail/{orderId}")
    public ResponseEntity<ApiResponse<OrderDetailDTO>> getOrder(
            @PathVariable("orderId") String orderIdStr,
            @AuthenticationPrincipal Long userId
    ) {
        Long realOrderId;
        try {
            if (orderIdStr.startsWith("ORDER_")) {
                realOrderId = Long.parseLong(orderIdStr.replace("ORDER_", ""));
            } else {
                realOrderId = Long.parseLong(orderIdStr);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("유효하지 않은 주문 ID 형식입니다: " + orderIdStr);
        }

        OrderDetailDTO orderDetail = orderService.getOrder(userId, realOrderId);
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