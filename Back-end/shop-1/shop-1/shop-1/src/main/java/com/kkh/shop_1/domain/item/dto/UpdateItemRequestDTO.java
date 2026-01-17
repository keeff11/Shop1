package com.kkh.shop_1.domain.item.dto;

import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateItemRequestDTO {
    private String name;
    private int price;
    private int quantity;
    private String category;
    private String description;
}
