package com.kkh.shop_1.domain.item.controller;

import com.kkh.shop_1.common.ApiResponse;
import com.kkh.shop_1.domain.item.dto.CreateItemRequestDTO;
import com.kkh.shop_1.domain.item.dto.ItemDetailDTO;
import com.kkh.shop_1.domain.item.dto.ItemSummaryDTO;
import com.kkh.shop_1.domain.item.service.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 *
 * 상품 관련 HTTP API 컨트롤러
 *
 */
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
    public ResponseEntity<ApiResponse<List<ItemSummaryDTO>>> getAllItems() {
        List<ItemSummaryDTO> items = itemService.getAllItems();
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