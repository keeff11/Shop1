package com.kkh.shop_1.domain.item.dto;

import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class CreateItemRequestDTO {

    private String name;
    private int price;
    private int quantity;
    private String category;
    private String description;
}