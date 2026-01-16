package com.kkh.shop_1.domain.order.repository;


import com.kkh.shop_1.domain.order.entity.Order;
import com.kkh.shop_1.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUser(User user);
}
