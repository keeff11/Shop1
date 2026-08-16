package com.kkh.shop_1.domain.outbox.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kkh.shop_1.domain.outbox.entity.OutboxEvent;
import com.kkh.shop_1.domain.outbox.entity.OutboxStatus;
import com.kkh.shop_1.domain.outbox.event.OutboxEventCreatedEvent;
import com.kkh.shop_1.domain.outbox.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OutboxEventServiceTest {

    private OutboxEventService outboxEventService;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private OutboxEventPublisher outboxEventPublisher;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private PlatformTransactionManager transactionManager;

    record SamplePayload(Long orderId, String message) {
    }

    @BeforeEach
    void setUp() {
        // ObjectMapper는 실제 직렬화 로직이 필요하므로 mock 대신 실제 인스턴스를 사용한다.
        // transactionManager는 mock이지만 TransactionTemplate이 콜백을 그대로 실행해주므로
        // 이 단위 테스트에서는 실제 트랜잭션 없이도 로직 검증이 가능하다.
        outboxEventService = new OutboxEventService(
                outboxEventRepository, outboxEventPublisher, applicationEventPublisher, new ObjectMapper(), transactionManager
        );
    }

    @Test
    @DisplayName("record()는 같은 트랜잭션에서 outbox 레코드를 저장하고, 커밋 후 처리를 위한 이벤트를 발행한다")
    void record_SavesEventAndPublishesApplicationEvent() {
        // given
        OutboxEvent saved = OutboxEvent.create("ORDER_PAID", "{}");
        ReflectionTestUtils.setField(saved, "id", 100L);
        given(outboxEventRepository.save(any(OutboxEvent.class))).willReturn(saved);

        // when
        Long resultId = outboxEventService.record("ORDER_PAID", new SamplePayload(1L, "hello"));

        // then
        assertThat(resultId).isEqualTo(100L);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo("ORDER_PAID");
        assertThat(captor.getValue().getPayload()).contains("\"orderId\":1").contains("\"message\":\"hello\"");

        ArgumentCaptor<OutboxEventCreatedEvent> eventCaptor = ArgumentCaptor.forClass(OutboxEventCreatedEvent.class);
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getOutboxEventId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("tryPublish() 성공 시 이벤트를 PUBLISHED로 변경한다")
    void tryPublish_Success_MarksPublished() {
        // given
        OutboxEvent event = OutboxEvent.create("ORDER_PAID", "{}");
        given(outboxEventRepository.findById(1L)).willReturn(Optional.of(event));

        // when
        outboxEventService.tryPublish(1L);

        // then
        verify(outboxEventPublisher).publish("ORDER_PAID", "{}");
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
    }

    @Test
    @DisplayName("tryPublish() 실패 시 예외를 삼키고 재시도 횟수를 늘린다 (호출자에게 예외를 전파하지 않음)")
    void tryPublish_Failure_IncrementsRetryCountWithoutThrowing() {
        // given
        OutboxEvent event = OutboxEvent.create("ORDER_PAID", "{}");
        given(outboxEventRepository.findById(1L)).willReturn(Optional.of(event));
        doThrow(new RuntimeException("발송 실패")).when(outboxEventPublisher).publish(any(), any());

        // when
        outboxEventService.tryPublish(1L);

        // then
        assertThat(event.getRetryCount()).isEqualTo(1);
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
    }

    @Test
    @DisplayName("이미 PUBLISHED인 이벤트는 다시 발행하지 않는다")
    void tryPublish_AlreadyPublished_SkipsRepublish() {
        // given
        OutboxEvent event = OutboxEvent.create("ORDER_PAID", "{}");
        event.markPublished();
        given(outboxEventRepository.findById(1L)).willReturn(Optional.of(event));

        // when
        outboxEventService.tryPublish(1L);

        // then
        verify(outboxEventPublisher, never()).publish(any(), any());
    }

    @Test
    @DisplayName("findStalePendingEventIds()는 PENDING 상태 이벤트의 ID 목록을 반환한다")
    void findStalePendingEventIds_ReturnsIds() {
        // given
        OutboxEvent event1 = OutboxEvent.create("ORDER_PAID", "{}");
        ReflectionTestUtils.setField(event1, "id", 1L);
        OutboxEvent event2 = OutboxEvent.create("ORDER_PAID", "{}");
        ReflectionTestUtils.setField(event2, "id", 2L);

        given(outboxEventRepository.findByStatusAndCreatedAtBefore(eq(OutboxStatus.PENDING), any(LocalDateTime.class)))
                .willReturn(List.of(event1, event2));

        // when
        List<Long> ids = outboxEventService.findStalePendingEventIds();

        // then
        assertThat(ids).containsExactly(1L, 2L);
    }
}
