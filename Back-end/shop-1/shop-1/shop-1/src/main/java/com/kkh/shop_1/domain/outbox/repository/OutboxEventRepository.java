package com.kkh.shop_1.domain.outbox.repository;

import com.kkh.shop_1.domain.outbox.entity.OutboxEvent;
import com.kkh.shop_1.domain.outbox.entity.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent> findByStatusAndCreatedAtBefore(OutboxStatus status, LocalDateTime threshold);
}
