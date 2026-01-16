package com.kkh.shop_1.domain.item.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StockStatus {
    IN_STOCK("재고 있음"),
    OUT_OF_STOCK("품절"),
    LIMITED("재고 소량 남음");

    private final String description;
}