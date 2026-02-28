package com.kkh.shop_1.domain.item.controller;

import com.kkh.shop_1.common.ApiResponse;
import com.kkh.shop_1.domain.item.dto.*;
import com.kkh.shop_1.domain.item.service.ItemSearchService;
import com.kkh.shop_1.domain.item.service.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/items")
public class ItemController {

    private final ItemService itemService;
    private final ItemSearchService itemSearchService;

    /**
     *
     * 상품 등록
     *
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Long>> createItem(
            @RequestPart("request") CreateItemRequestDTO request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @AuthenticationPrincipal Long sellerId
    ) throws IOException {
        Long itemId = itemService.createItem(request, images, sellerId);
        return ResponseEntity.ok(ApiResponse.success(itemId));
    }

    /**
     *
     * 상품 수정
     *
     */
    @PutMapping("/{itemId}")
    public ResponseEntity<ApiResponse<Long>> updateItem(
            @PathVariable Long itemId,
            @RequestPart("request") UpdateItemRequestDTO updateItemRequestDTO,
            @RequestPart(value = "images", required = false) List<MultipartFile> image,
            @AuthenticationPrincipal Long sellerId
    ) {
        Long updateItemId = itemService.updateItem(itemId, updateItemRequestDTO, image, sellerId);
        return ResponseEntity.ok(ApiResponse.success(updateItemId));
    }

    /**
     *
     * 상품 삭제
     *
     */
    @DeleteMapping("/{itemId}")
    public ResponseEntity<ApiResponse<String>> deleteItem(
            @PathVariable Long itemId,
            @AuthenticationPrincipal Long userId // 로그인된 사용자 ID 주입
    ) {
        itemService.deleteItem(itemId, userId);
        return ResponseEntity.ok(ApiResponse.success("상품이 성공적으로 삭제되었습니다."));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<ItemSummaryDTO>>> getAllItemsForTest() {
        // Service의 @Cacheable 붙은 메서드를 직접 호출
        List<ItemSummaryDTO> items = itemService.getAllItems();
        return ResponseEntity.ok(ApiResponse.success(items));
    }

    /**
     *
     * 내가 등록한 상품 목록 조회
     *
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<ItemSummaryDTO>>> getMyItems(
            @AuthenticationPrincipal Long sellerId) {

        List<ItemSummaryDTO> myItems = itemService.getMyItems(sellerId);
        return ResponseEntity.ok(ApiResponse.success(myItems));
    }


    /**
     *
     * 상품 상세 조회
     *
     */
    @GetMapping("/{itemId}")
    public ResponseEntity<ApiResponse<ItemDetailDTO>> getItem(@PathVariable Long itemId) {
        ItemDetailDTO dto = itemService.getItemDetail(itemId);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    /**
     *
     * 전체 상품 목록 조회
     *
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ItemSummaryDTO>>> getItems(
            @ModelAttribute ItemSearchCondition condition,
            @PageableDefault(size = 12) Pageable pageable
    ) {
        Page<ItemSummaryDTO> items = itemService.searchItems(condition, pageable);
        return ResponseEntity.ok(ApiResponse.success(items));
    }

    /**
     *
     * 카테고리별 상품 목록 조회
     *
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<ApiResponse<List<ItemSummaryDTO>>> getItemsByCategory(@PathVariable String category) {
        List<ItemSummaryDTO> items = itemService.getItemsByCategory(category);
        return ResponseEntity.ok(ApiResponse.success(items));
    }

    @GetMapping("/ranking")
    public ResponseEntity<ApiResponse<List<ItemSummaryDTO>>> getPopularItems() {
        List<ItemSummaryDTO> popularItems = itemService.getPopularItems();
        return ResponseEntity.ok(ApiResponse.success(popularItems));
    }

    /**
     * 🌟 [추가] 최근 본 상품 기록하기 (가벼운 비동기 호출용)
     */
    @PostMapping("/{itemId}/recent")
    public ResponseEntity<ApiResponse<Void>> addRecentItem(
            @PathVariable Long itemId,
            @RequestHeader(value = "Viewer-Id", required = false) String viewerId) {

        if (viewerId == null || viewerId.isEmpty()) return ResponseEntity.ok().build();

        itemService.addRecentItem(viewerId, itemId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 🌟 [추가] 최근 본 상품 목록 조회
     */
    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<ItemSummaryDTO>>> getRecentItems(
            @RequestHeader(value = "Viewer-Id", required = false) String viewerId) {

        if (viewerId == null || viewerId.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success(Collections.emptyList()));
        }

        List<ItemSummaryDTO> recentItems = itemService.getRecentItems(viewerId);
        return ResponseEntity.ok(ApiResponse.success(recentItems));
    }

    // 🌟 실시간 검색어 자동완성 API
    @GetMapping("/search/autocomplete")
    public ResponseEntity<ApiResponse<List<ItemSummaryDTO>>> autocomplete(@RequestParam String keyword) {
        List<ItemSummaryDTO> suggestions = itemSearchService.getAutocompleteSuggestions(keyword);
        return ResponseEntity.ok(ApiResponse.success(suggestions));
    }

    // 🌟 (관리자용) MySQL -> ES 데이터 동기화 트리거
    @PostMapping("/search/sync")
    public ResponseEntity<ApiResponse<String>> syncToElasticsearch() {
        itemSearchService.syncItemsToElasticsearch();
        return ResponseEntity.ok(ApiResponse.success("Elasticsearch 데이터 동기화 완료"));
    }

}