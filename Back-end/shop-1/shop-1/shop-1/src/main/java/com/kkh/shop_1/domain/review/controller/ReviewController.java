package com.kkh.shop_1.domain.review.controller;

import com.kkh.shop_1.common.ApiResponse;
import com.kkh.shop_1.domain.review.dto.ReviewRequestDto;
import com.kkh.shop_1.domain.review.dto.ReviewResponseDto;
import com.kkh.shop_1.domain.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/reviews") // 경로 단순화 (RESTful)
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * 리뷰 작성 API
     * POST /reviews?orderItemId={id}
     * Content-Type: multipart/form-data
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Long>> create(
            @RequestParam Long orderItemId,
            @AuthenticationPrincipal Long userId,
            @RequestPart("review") ReviewRequestDto dto,
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) {
        Long reviewId = reviewService.createReview(orderItemId, userId, dto, images);
        return ResponseEntity.ok(ApiResponse.success(reviewId));
    }

    /**
     * 리뷰 삭제 API
     */
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal Long userId
    ) {
        reviewService.deleteReview(reviewId, userId);
        return ResponseEntity.ok(ApiResponse.successNoData());
    }

    /**
     * 상품별 리뷰 조회 API
     * GET /reviews/items/{itemId}
     */
    @GetMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<Page<ReviewResponseDto>>> getReviews(
            @PathVariable Long itemId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal Long userId // 로그인 안 했으면 null (Security 설정 필요)
    ) {
        // 비로그인 사용자도 리뷰는 볼 수 있어야 하므로 userId null 처리 주의
        Page<ReviewResponseDto> reviews = reviewService.getReviewsByItem(itemId, pageable, userId);
        return ResponseEntity.ok(ApiResponse.success(reviews));
    }
}