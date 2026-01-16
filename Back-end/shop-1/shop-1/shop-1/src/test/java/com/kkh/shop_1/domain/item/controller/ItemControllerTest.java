//package com.kkh.shop_1.domain.item.controller;
//
//import com.kkh.shop_1.domain.item.dto.ItemResponseDTO;
//import com.kkh.shop_1.domain.item.entity.Item;
//import com.kkh.shop_1.domain.item.service.ItemService;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.test.web.servlet.MockMvc;
//
//import java.util.List;
//
//import static org.mockito.BDDMockito.given;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
//@SpringBootTest
//@AutoConfigureMockMvc
//class ItemControllerTest {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @MockBean
//    private ItemService itemService;
//
//    // -------------------------
//    // 상품 전체 조회
//    // -------------------------
//    @Test
//    @DisplayName("상품 전체 조회 성공")
//    void getAllItems_success() throws Exception {
//        // given
//        ItemResponseDTO dto = ItemResponseDTO.builder()
//                .id(1L)
//                .name("테스트 상품")
//                .price(10000)
//                .images(List.of("http://image.com/1.png"))
//                .status("SELLING")
//                .build();
//
//        given(itemService.getAllItems()).willReturn(List.of(dto));
//
//        // when & then
//        mockMvc.perform(get("/items"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success").value(true))
//                .andExpect(jsonPath("$.data[0].id").value(1L))
//                .andExpect(jsonPath("$.data[0].name").value("테스트 상품"))
//                .andExpect(jsonPath("$.data[0].price").value(10000))
//                .andExpect(jsonPath("$.data[0].images[0]").value("http://image.com/1.png"))
//                .andExpect(jsonPath("$.data[0].status").value("SELLING"));
//    }
//
//    @Test
//    @DisplayName("상품 전체 조회 - 빈 리스트")
//    void getAllItems_emptyList_success() throws Exception {
//        // given
//        given(itemService.getAllItems()).willReturn(List.of());
//
//        // when & then
//        mockMvc.perform(get("/items"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success").value(true))
//                .andExpect(jsonPath("$.data").isArray())
//                .andExpect(jsonPath("$.data").isEmpty());
//    }
//
//    @Test
//    @DisplayName("상품 전체 조회 실패 - 서비스 예외")
//    void getAllItems_serviceException_fail() throws Exception {
//        // given
//        given(itemService.getAllItems()).willThrow(new RuntimeException("DB 오류"));
//
//        // when & then
//        mockMvc.perform(get("/items"))
//                .andExpect(status().is5xxServerError())
//                .andExpect(jsonPath("$.success").value(false))
//                .andExpect(jsonPath("$.message").value("DB 오류"));
//    }
//
//    // -------------------------
//    // 단건 조회 테스트
//    // -------------------------
//    @Test
//    @DisplayName("상품 단건 조회 성공")
//    void getItemById_success() throws Exception {
//        // given: 엔티티 반환
//        Item item = Item.builder()
//                .id(1L)
//                .name("단건 상품")
//                .price(5000)
//                .quantity(10)
//                .stockStatus(Item.StockStatus.IN_STOCK)
//                .status(Item.ItemStatus.SELLING)
//                .build();
//
//        given(itemService.getItemById(1L)).willReturn(item); // 엔티티 반환
//
//        // when & then: 컨트롤러에서 DTO 변환 후 검증
//        mockMvc.perform(get("/items/1"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success").value(true))
//                .andExpect(jsonPath("$.data.id").value(1L))
//                .andExpect(jsonPath("$.data.name").value("단건 상품"))
//                .andExpect(jsonPath("$.data.price").value(5000))
//                .andExpect(jsonPath("$.data.images").isArray())
//                .andExpect(jsonPath("$.data.status").value("SELLING"));
//    }
//
//    @Test
//    @DisplayName("상품 단건 조회 실패 - 존재하지 않음")
//    void getItemById_fail() throws Exception {
//        // given
//        given(itemService.getItemById(1L))
//                .willThrow(new RuntimeException("아이템을 찾을 수 없습니다"));
//
//        // when & then
//        mockMvc.perform(get("/items/1"))
//                .andExpect(status().isNotFound())
//                .andExpect(jsonPath("$.success").value(false))
//                .andExpect(jsonPath("$.message").value("아이템을 찾을 수 없습니다"));
//    }
//}
