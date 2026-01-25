package com.kkh.shop_1.domain.review.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kkh.shop_1.domain.review.dto.ReviewRequestDto;
import com.kkh.shop_1.domain.review.service.ReviewService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReviewController.class)
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReviewService reviewService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("리뷰 등록 성공")
    @WithMockUser(username = "1", roles = "USER")
    void createReview_Success() throws Exception {
        // given
        Long orderItemId = 1L;
        ReviewRequestDto requestDto = new ReviewRequestDto(5, "좋아요");
        MockMultipartFile reviewPart = new MockMultipartFile("review", "", "application/json", objectMapper.writeValueAsString(requestDto).getBytes(StandardCharsets.UTF_8));
        MockMultipartFile imagePart = new MockMultipartFile("images", "image.jpg", "image/jpeg", "image data".getBytes());

        given(reviewService.createReview(eq(orderItemId), any(), any(), any())).willReturn(100L);

        // when & then
        mockMvc.perform(multipart("/reviews")
                        .file(reviewPart)
                        .file(imagePart)
                        .param("orderItemId", String.valueOf(orderItemId))
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(100L));
    }

    @Test
    @DisplayName("리뷰 등록 실패 - 필수 파라미터 누락 (orderItemId)")
    @WithMockUser(username = "1", roles = "USER")
    void createReview_Fail_MissingParam() throws Exception {
        // given
        ReviewRequestDto requestDto = new ReviewRequestDto(5, "좋아요");
        MockMultipartFile reviewPart = new MockMultipartFile("review", "", "application/json", objectMapper.writeValueAsString(requestDto).getBytes(StandardCharsets.UTF_8));

        // when & then
        mockMvc.perform(multipart("/reviews")
                        .file(reviewPart)
                        // .param("orderItemId", ...) // 누락
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }
}
