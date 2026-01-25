package com.kkh.shop_1.domain.order.repository;

import com.kkh.shop_1.domain.order.entity.Order;
import com.kkh.shop_1.domain.order.entity.OrderItem;
import com.kkh.shop_1.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // [핵심 수정] N+1 문제 해결: 주문 목록 조회 시 주문상품과 상품 정보를 한 번에 가져옴
    @Query("SELECT DISTINCT o FROM Order o " +
            "JOIN FETCH o.orderItems oi " +
            "JOIN FETCH oi.item " +
            "WHERE o.user = :user " +
            "ORDER BY o.orderDate DESC")
    List<Order> findByUserWithFetch(@Param("user") User user);

    // 기존 메서드 유지 (필요 시 Fetch Join 적용 고려 가능)
    List<Order> findByUser(User user);

    @Query("select oi from OrderItem oi where oi.id = :orderItemId")
    Optional<OrderItem> findOrderItemById(@Param("orderItemId") Long orderItemId);
}