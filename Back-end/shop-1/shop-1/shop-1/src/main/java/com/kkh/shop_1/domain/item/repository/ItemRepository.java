package com.kkh.shop_1.domain.item.repository;

import com.kkh.shop_1.domain.item.entity.Item;
import com.kkh.shop_1.domain.item.entity.ItemCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ItemRepository extends JpaRepository<Item, Long> {

    List<Item> findBySellerIdOrderByCreatedAtDesc(Long sellerId);

    List<Item> findByItemCategory(ItemCategory itemCategory);

    // [핵심 수정] N+1 문제 해결을 위한 Fetch Join (전체 상품 조회 시 이미지도 한 번에 로딩)
    @Query("SELECT DISTINCT i FROM Item i LEFT JOIN FETCH i.images")
    List<Item> findAllWithImages();

    // [핵심 수정] 동시성 해결: 재고 차감 (DB 레벨에서 원자적 실행)
    // 반환값: 업데이트된 행의 수 (0이면 재고 부족으로 실패)
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Item i SET i.quantity = i.quantity - :count WHERE i.id = :id AND i.quantity >= :count")
    int decreaseStock(@Param("id") Long id, @Param("count") int count);

    // [핵심 수정] 동시성 해결: 조회수 증가 (DB 레벨에서 실행)
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Item i SET i.viewCount = i.viewCount + 1 WHERE i.id = :id")
    void increaseViewCount(@Param("id") Long id);
}