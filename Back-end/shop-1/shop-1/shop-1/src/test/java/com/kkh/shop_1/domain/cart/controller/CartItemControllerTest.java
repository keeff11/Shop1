package com.kkh.shop_1.domain.cart.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kkh.shop_1.common.ApiResponse;
import com.kkh.shop_1.domain.cart.dto.CartItemAddRequestDTO;
import com.kkh.shop_1.domain.cart.dto.CartItemResponseDTO;
import com.kkh.shop_1.domain.cart.service.CartItemService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CartItemController.class)
class CartItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CartItemService cartItemService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("장바구니 추가 성공")
    @WithMockUser(username = "1", roles = "USER")
    void addCartItem_Success() throws Exception {
        // given
        CartItemAddRequestDTO request = new CartItemAddRequestDTO(1L, 2);

        // when & then
        mockMvc.perform(post("/cart/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        verify(cartItemService).addItemToCart(eq(1L), any(CartItemAddRequestDTO.class));
    }

    @Test
    @DisplayName("장바구니 수량 감소 성공")
    @WithMockUser(username = "1", roles = "USER")
    void decreaseCartItem_Success() throws Exception {
        // given
        Long itemId = 1L;

        // when & then
        mockMvc.perform(post("/cart/decrease/{itemId}", itemId)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        verify(cartItemService).decreaseItemQuantity(1L, itemId);
    }

    @Test
    @DisplayName("장바구니 아이템 삭제 성공")
    @WithMockUser(username = "1", roles = "USER")
    void removeCartItem_Success() throws Exception {
        // given
        Long itemId = 1L;

        // when & then
        mockMvc.perform(post("/cart/remove/{itemId}", itemId)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        verify(cartItemService).removeItemFromCart(1L, itemId);
    }

    @Test
    @DisplayName("내 장바구니 조회 성공")
    @WithMockUser(username = "1", roles = "USER")
    void getCartItems_Success() throws Exception {
        // given
        CartItemResponseDTO item = new CartItemResponseDTO(1L, 1L, "상품명", 1000, 2, "img.jpg");
        List<CartItemResponseDTO> items = List.of(item);

        given(cartItemService.getCartItems(1L)).willReturn(ApiResponse.success(items));

        // when & then
        mockMvc.perform(get("/cart/list")
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].itemId").value(1L))
                .andExpect(jsonPath("$.data[0].itemName").value("상품명"));
    }

    @Test
    @DisplayName("내 장바구니 조회 - 비로그인 시 빈 리스트 반환")
    void getCartItems_NoLogin() throws Exception {
        // when & then
        mockMvc.perform(get("/cart/list"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
    }
}
