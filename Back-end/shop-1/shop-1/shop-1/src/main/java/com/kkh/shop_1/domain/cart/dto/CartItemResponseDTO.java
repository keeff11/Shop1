package com.kkh.shop_1.domain.cart.dto;

import lombok.*;

@Getter
@AllArgsConstructor
public class CartItemResponseDTO {
    private Long itemId;
    private String itemName;
    private int price;
    private int quantity;
}
