package com.kkh.shop_1.domain.item.controller;

import com.kkh.shop_1.common.ApiResponse;
import com.kkh.shop_1.domain.item.dto.*;
import com.kkh.shop_1.domain.item.service.ItemService;
import lombok.RequiredArgsConstructor;
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
    public ResponseEntity<ApiResponse<Void>> deleteItem(
            @PathVariable Long itemId,
            @AuthenticationPrincipal Long sellerId) { // 시큐리티에서 유저 정보 주입

        itemService.deleteItem(itemId, sellerId);
        return ResponseEntity.ok(ApiResponse.successNoData());
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
    public ResponseEntity<ApiResponse<List<ItemSummaryDTO>>> getItems(
            @ModelAttribute ItemSearchCondition condition
    ) {
        List<ItemSummaryDTO> items = itemService.searchItems(condition);
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