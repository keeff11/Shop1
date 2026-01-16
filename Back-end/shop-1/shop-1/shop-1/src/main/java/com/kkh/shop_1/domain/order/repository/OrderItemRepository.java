package com.kkh.shop_1.domain.order.repository;

import com.kkh.shop_1.domain.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}
