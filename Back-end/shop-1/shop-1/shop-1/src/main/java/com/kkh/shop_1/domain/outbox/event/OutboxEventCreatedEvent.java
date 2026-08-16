package com.kkh.shop_1.domain.outbox.event;

import lombok.Getter;

/**
 *
 * outbox_event 테이블에 새 레코드가 저장된 직후(같은 트랜잭션 안에서) 발행되는 스프링 애플리케이션 이벤트.
 * AFTER_COMMIT 시점에 이 이벤트를 받아 즉시 발행을 시도한다.
 *
 */
@Getter
public class OutboxEventCreatedEvent {

    private final Long outboxEventId;

    public OutboxEventCreatedEvent(Long outboxEventId) {
        this.outboxEventId = outboxEventId;
    }
}
