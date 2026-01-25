package com.kkh.shop_1.domain.item.controller;

import com.kkh.shop_1.domain.item.dto.ItemDetailDTO;
import com.kkh.shop_1.domain.item.dto.ItemSummaryDTO;
import com.kkh.shop_1.domain.item.entity.Item;
import com.kkh.shop_1.domain.item.entity.ItemCategory;
import com.kkh.shop_1.domain.item.entity.ItemStatus;
import com.kkh.shop_1.domain.item.entity.StockStatus;
import com.kkh.shop_1.domain.item.service.ItemService;
import com.kkh.shop_1.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ItemController.class)
@AutoConfigureMockMvc
class ItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ItemService itemService;

    // -------------------------
    // 상품 전체 조회
    // -------------------------
    @Test
    @DisplayName("상품 전체 조회 성공")
    @WithMockUser
    void getAllItems_success() throws Exception {
        // given
        ItemSummaryDTO dto = ItemSummaryDTO.builder()
                .id(1L)
                .name("테스트 상품")
                .price(10000)
                .thumbnailUrl("http://image.com/1.png")
                .status("SELLING")
                .build();

        given(itemService.getAllItems()).willReturn(List.of(dto));

        // when & then
        mockMvc.perform(get("/items")
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data[0].id").value(1L))
                .andExpect(jsonPath("$.data[0].name").value("테스트 상품"))
                .andExpect(jsonPath("$.data[0].price").value(10000))
                .andExpect(jsonPath("$.data[0].thumbnailUrl").value("http://image.com/1.png"))
                .andExpect(jsonPath("$.data[0].status").value("SELLING"));
    }

    @Test
    @DisplayName("상품 전체 조회 - 빈 리스트")
    @WithMockUser
    void getAllItems_emptyList_success() throws Exception {
        // given
        given(itemService.getAllItems()).willReturn(List.of());

        // when & then
        mockMvc.perform(get("/items")
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    // -------------------------
    // 단건 조회 테스트
    // -------------------------
    @Test
    @DisplayName("상품 단건 조회 성공")
    @WithMockUser
    void getItemById_success() throws Exception {
        // given: DTO 반환
        ItemDetailDTO dto = ItemDetailDTO.builder()
                .id(1L)
                .name("단건 상품")
                .price(5000)
                .quantity(10)
                .category("CLOTHING")
                .description("설명")
                .sellerNickname("판매자")
                .status("SELLING")
                .build();

        given(itemService.getItemDetail(1L)).willReturn(dto);

        // when & then
        mockMvc.perform(get("/items/1")
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.name").value("단건 상품"))
                .andExpect(jsonPath("$.data.price").value(5000))
                .andExpect(jsonPath("$.data.status").value("SELLING"));
    }

    @Test
    @DisplayName("상품 단건 조회 실패 - 존재하지 않음")
    @WithMockUser
    void getItemById_fail() throws Exception {
        // given
        given(itemService.getItemDetail(1L))
                .willThrow(new IllegalArgumentException("아이템을 찾을 수 없습니다"));

        // when & then
        mockMvc.perform(get("/items/1")
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isBadRequest()) // GlobalExceptionHandler에 따라 다를 수 있음
                .andExpect(jsonPath("$.status").value("FAIL"))
                .andExpect(jsonPath("$.message").value("아이템을 찾을 수 없습니다"));
    }
}
