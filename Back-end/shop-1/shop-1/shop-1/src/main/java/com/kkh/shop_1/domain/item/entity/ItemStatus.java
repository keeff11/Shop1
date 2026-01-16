package com.kkh.shop_1.domain.item.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ItemStatus {
    SELLING("판매 중"),
    SOLD_OUT("판매 중지(품절)"),
    HIDDEN("비노출");

    private final String description;
}