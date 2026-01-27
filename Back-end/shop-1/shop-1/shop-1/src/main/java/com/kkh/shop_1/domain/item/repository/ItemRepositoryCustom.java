package com.kkh.shop_1.domain.item.repository;

import com.kkh.shop_1.domain.item.dto.ItemSearchCondition;
import com.kkh.shop_1.domain.item.entity.Item;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ItemRepositoryCustom {
    // [수정] List<Item> -> Page<Item>으로 변경 및 Pageable 파라미터 추가
    Page<Item> search(ItemSearchCondition condition, Pageable pageable);
}