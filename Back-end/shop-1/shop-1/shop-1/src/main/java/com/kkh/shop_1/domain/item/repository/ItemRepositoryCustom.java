package com.kkh.shop_1.domain.item.repository;

import com.kkh.shop_1.domain.item.dto.ItemSearchCondition;
import com.kkh.shop_1.domain.item.entity.Item;
import java.util.List;

public interface ItemRepositoryCustom {
    List<Item> search(ItemSearchCondition condition);
}