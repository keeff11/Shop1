package com.kkh.shop_1.domain.item.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ItemCategory {

    ELECTRONICS("전자기기"),
    CLOTHING("의류"),
    HOME("가전/생활"),
    BOOKS("도서"),
    BEAUTY("뷰티/화장품"),
    OTHERS("기타");

    private final String displayName;
}
