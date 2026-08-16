package com.kkh.shop_1.domain.outbox.service;

import com.kkh.shop_1.domain.outbox.event.OutboxEventCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 *
 * 트랜잭션 커밋 직후 outbox 이벤트를 즉시 발행 시도한다. (실패하거나 유실되면 OutboxPollingScheduler가 재처리)
 *
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventListener {

    private final OutboxEventService outboxEventService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOutboxEventCreated(OutboxEventCreatedEvent event) {
        log.info("트랜잭션 커밋 완료, Outbox 이벤트 즉시 발행 시도. outboxEventId={}", event.getOutboxEventId());
        outboxEventService.tryPublish(event.getOutboxEventId());
    }
}
