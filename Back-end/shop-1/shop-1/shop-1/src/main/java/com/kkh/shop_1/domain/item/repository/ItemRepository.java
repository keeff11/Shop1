package com.kkh.shop_1.domain.item.repository;

import com.kkh.shop_1.domain.item.entity.Item;
import com.kkh.shop_1.domain.item.entity.ItemCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {

    List<Item> findByItemCategory(ItemCategory itemCategory);

    List<Item> findBySellerIdOrderByCreatedAtDesc(Long sellerId);
}
