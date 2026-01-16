package com.kkh.shop_1.domain.cart.repository;

import com.kkh.shop_1.domain.cart.entity.CartItem;
import com.kkh.shop_1.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByUser(User user);

    Optional<CartItem> findByUserAndItemId(User user, Long itemId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM CartItem c WHERE c.user = :user AND c.itemId = :itemId")
    void deleteByUserAndItemId(@Param("user") User user, @Param("itemId") Long itemId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM CartItem c WHERE c.user = :user")
    void deleteAllByUser(@Param("user") User user);

    List<CartItem> findByUserAndItemIdIn(User user, List<Long> itemIds);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM CartItem c WHERE c.user = :user AND c.itemId IN :itemIds")
    void deleteSelectedItemsByUser(@Param("user") User user, @Param("itemIds") List<Long> itemIds);

}