package com.kkh.shop_1.domain.order.repository;

import com.kkh.shop_1.domain.order.entity.Order;
import com.kkh.shop_1.domain.order.entity.OrderItem;
import com.kkh.shop_1.domain.order.entity.OrderStatus;
import com.kkh.shop_1.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("SELECT DISTINCT o FROM Order o " +
            "JOIN FETCH o.orderItems oi " +
            "JOIN FETCH oi.item " +
            "JOIN FETCH o.address " + // ★ 추가됨
            "WHERE o.user = :user " +
            "ORDER BY o.orderDate DESC")
    List<Order> findByUserWithFetch(@Param("user") User user);

    @Query("SELECT DISTINCT o FROM Order o " +
            "JOIN FETCH o.orderItems oi " +
            "JOIN FETCH oi.item " +
            "JOIN FETCH o.address " +
            "WHERE o.id = :orderId")
    Optional<Order> findByIdWithFetch(@Param("orderId") Long orderId);

    List<Order> findByUser(User user);

    @Query("select oi from OrderItem oi where oi.id = :orderItemId")
    Optional<OrderItem> findOrderItemById(@Param("orderItemId") Long orderItemId);

    /**
     * 정합성 배치용: 특정 상태로 일정 시간 이상 머물러 있는 주문 (예: PAYMENT_PENDING으로 오래 방치된 주문)
     */
    List<Order> findByStatusAndOrderDateBefore(OrderStatus status, LocalDateTime threshold);
}