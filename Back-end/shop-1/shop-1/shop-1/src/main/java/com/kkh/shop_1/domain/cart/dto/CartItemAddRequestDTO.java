package com.kkh.shop_1.domain.cart.dto;

import lombok.*;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CartItemAddRequestDTO {
    private Long itemId;
    private int quantity;
}
