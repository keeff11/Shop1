package com.kkh.shop_1.domain.review.repository;

import com.kkh.shop_1.domain.review.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    @Query("select r from Review r join fetch r.user left join fetch r.images where r.item.id = :itemId order by r.createdAt desc")
    Page<Review> findAllByItemId(@Param("itemId") Long itemId, Pageable pageable);
}