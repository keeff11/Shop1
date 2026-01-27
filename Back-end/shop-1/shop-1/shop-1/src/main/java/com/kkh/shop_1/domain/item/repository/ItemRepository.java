package com.kkh.shop_1.domain.item.repository;

import com.kkh.shop_1.domain.item.entity.Item;
import com.kkh.shop_1.domain.item.entity.ItemCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

// [수정] ItemRepositoryCustom 상속 추가
public interface ItemRepository extends JpaRepository<Item, Long>, ItemRepositoryCustom {

    List<Item> findBySellerIdOrderByCreatedAtDesc(Long sellerId);

    List<Item> findByItemCategory(ItemCategory itemCategory);

    // 기존 JPQL 방식도 유지 가능하지만, 동적 쿼리로 대체 가능
    @Query("SELECT DISTINCT i FROM Item i LEFT JOIN FETCH i.images")
    List<Item> findAllWithImages();

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Item i SET i.quantity = i.quantity - :count WHERE i.id = :id AND i.quantity >= :count")
    int decreaseStock(@Param("id") Long id, @Param("count") int count);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Item i SET i.viewCount = i.viewCount + 1 WHERE i.id = :id")
    void increaseViewCount(@Param("id") Long id);
}