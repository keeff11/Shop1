package com.kkh.shop_1.domain.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kkh.shop_1.domain.order.dto.OrderDetailDTO;
import com.kkh.shop_1.domain.order.dto.OrderRequestDTO;
import com.kkh.shop_1.domain.order.dto.OrderResponseDTO;
import com.kkh.shop_1.domain.order.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("주문 생성 성공")
    @WithMockUser(username = "1", roles = "USER")
    void createOrder_Success() throws Exception {
        // given
        OrderRequestDTO request = new OrderRequestDTO(); // 필드 세팅 필요 시 추가
        OrderResponseDTO response = OrderResponseDTO.builder()
                .orderId(1L)
                .tid("tid")
                .redirectUrl("url")
                .orderDate(LocalDateTime.now())
                .build();

        given(orderService.orderItems(eq(1L), any(OrderRequestDTO.class))).willReturn(response);

        // when & then
        mockMvc.perform(post("/orders/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderId").value(1L));
    }

    @Test
    @DisplayName("결제 승인 성공")
    @WithMockUser(username = "1", roles = "USER")
    void approveOrder_Success() throws Exception {
        // given
        OrderDetailDTO response = OrderDetailDTO.builder()
                .orderId(1L)
                .totalPrice(1000)
                .status("PAID")
                .build();
        given(orderService.approveOrder(1L, "token", 1L)).willReturn(response);

        // when & then
        mockMvc.perform(get("/orders/payment/approve")
                        .param("orderId", "1")
                        .param("pg_token", "token")
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderId").value(1L));
    }

    @Test
    @DisplayName("주문 상세 조회 성공")
    @WithMockUser(username = "1", roles = "USER")
    void getOrder_Success() throws Exception {
        // given
        OrderDetailDTO response = OrderDetailDTO.builder()
                .orderId(1L)
                .totalPrice(1000)
                .status("PAID")
                .build();
        given(orderService.getOrder(1L, 1L)).willReturn(response);

        // when & then
        mockMvc.perform(get("/orders/detail/{orderId}", 1L)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderId").value(1L));
    }

    @Test
    @DisplayName("주문 목록 조회 성공")
    @WithMockUser(username = "1", roles = "USER")
    void getOrders_Success() throws Exception {
        // given
        OrderDetailDTO orderDetail = OrderDetailDTO.builder()
                .orderId(1L)
                .totalPrice(1000)
                .status("PAID")
                .build();
        List<OrderDetailDTO> response = Collections.singletonList(orderDetail);
        given(orderService.getOrders(1L)).willReturn(response);

        // when & then
        mockMvc.perform(get("/orders/list")
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].orderId").value(1L));
    }
}
