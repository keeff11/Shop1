package com.kkh.shop_1.domain.cart.controller;

import com.kkh.shop_1.common.ApiResponse;
import com.kkh.shop_1.domain.cart.dto.CartItemAddRequestDTO;
import com.kkh.shop_1.domain.cart.dto.CartItemResponseDTO;
import com.kkh.shop_1.domain.cart.service.CartItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartItemController {

    private final CartItemService cartItemService;


    /**
     *
     * 장바구니 추가 (담기, +수량 1)
     *
     */
    @PostMapping("/add")
    public ApiResponse<Void> addCartItem(
            @RequestBody CartItemAddRequestDTO request,
            @AuthenticationPrincipal Long userId
    ) {
        cartItemService.addItemToCart(userId, request);
        return ApiResponse.successNoData();
    }

    /**
     *
     * 수량 -1
     *
     */
    @PostMapping("/decrease/{itemId}")
    public ApiResponse<Void> decreaseCartItem(
            @PathVariable Long itemId,
            @AuthenticationPrincipal Long userId
    ) {
        cartItemService.decreaseItemQuantity(userId, itemId);
        return ApiResponse.successNoData();
    }

    /**
     *
     * 장바구니 리스트에서 삭제
     *
     */
    @PostMapping("/remove/{itemId}")
    public ApiResponse<Void> removeCartItem(
            @PathVariable Long itemId,
            @AuthenticationPrincipal Long userId
    ) {
        cartItemService.removeItemFromCart(userId, itemId);
        return ApiResponse.successNoData();
    }

    /**
     *
     * 내 장바구니 조회
     *
     */
    @GetMapping("/list")
    public ApiResponse<List<CartItemResponseDTO>> getCartItems(@AuthenticationPrincipal Long userId) {
        if (userId == null) {
            return ApiResponse.success(Collections.emptyList());
        }
        return cartItemService.getCartItems(userId);
    }
}