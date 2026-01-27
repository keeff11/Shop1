package com.kkh.shop_1.domain.item.dto;

import lombok.Data;

@Data
public class ItemSearchCondition {
    private String keyword;    // 검색어 (상품명, 설명)
    private String category;   // 카테고리
    private Integer minPrice;  // 최소 가격
    private Integer maxPrice;  // 최대 가격
    private String sort;       // 정렬 기준 (latest, priceHigh, priceLow, views)
}