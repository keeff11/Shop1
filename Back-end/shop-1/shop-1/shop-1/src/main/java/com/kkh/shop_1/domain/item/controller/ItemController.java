package com.kkh.shop_1.domain.item.controller;

import com.kkh.shop_1.common.ApiResponse;
import com.kkh.shop_1.domain.item.dto.*;
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
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/items")
public class ItemController {

    private final ItemService itemService;

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

}