package com.kkh.shop_1.domain.outbox.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OutboxPollingSchedulerTest {

    @InjectMocks
    private OutboxPollingScheduler outboxPollingScheduler;

    @Mock
    private OutboxEventService outboxEventService;

    @Test
    @DisplayName("재처리 대상이 없으면 발행을 시도하지 않는다")
    void republishStaleEvents_NoStaleEvents_DoesNotTryPublish() {
        // given
        given(outboxEventService.findStalePendingEventIds()).willReturn(List.of());

        // when
        outboxPollingScheduler.republishStaleEvents();

        // then
        verify(outboxEventService, never()).tryPublish(anyLong());
    }

    @Test
    @DisplayName("재처리 대상이 있으면 각 id마다 한 번씩 발행을 시도한다")
    void republishStaleEvents_StaleEventsExist_TriesPublishEachOnce() {
        // given
        given(outboxEventService.findStalePendingEventIds()).willReturn(List.of(1L, 2L, 3L));

        // when
        outboxPollingScheduler.republishStaleEvents();

        // then
        verify(outboxEventService).tryPublish(1L);
        verify(outboxEventService).tryPublish(2L);
        verify(outboxEventService).tryPublish(3L);
    }
}
