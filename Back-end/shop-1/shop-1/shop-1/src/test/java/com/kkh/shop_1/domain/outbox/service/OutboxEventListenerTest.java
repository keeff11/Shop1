package com.kkh.shop_1.domain.outbox.service;

import com.kkh.shop_1.domain.outbox.event.OutboxEventCreatedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OutboxEventListenerTest {

    @InjectMocks
    private OutboxEventListener outboxEventListener;

    @Mock
    private OutboxEventService outboxEventService;

    @Test
    @DisplayName("트랜잭션 커밋 후 이벤트를 받으면 해당 outboxEventId로 즉시 발행을 시도한다")
    void onOutboxEventCreated_TriesPublishWithCorrectId() {
        // given
        OutboxEventCreatedEvent event = new OutboxEventCreatedEvent(42L);

        // when
        outboxEventListener.onOutboxEventCreated(event);

        // then
        verify(outboxEventService).tryPublish(42L);
    }
}
