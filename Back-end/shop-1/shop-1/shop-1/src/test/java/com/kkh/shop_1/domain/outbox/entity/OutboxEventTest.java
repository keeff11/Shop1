package com.kkh.shop_1.domain.outbox.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxEventTest {

    @Test
    @DisplayName("생성 직후에는 PENDING 상태이고 재시도 횟수는 0이다")
    void create_InitialStateIsPending() {
        // when
        OutboxEvent event = OutboxEvent.create("ORDER_PAID", "{}");

        // then
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(event.getRetryCount()).isEqualTo(0);
        assertThat(event.isPublished()).isFalse();
    }

    @Test
    @DisplayName("발행 성공 처리하면 PUBLISHED 상태가 되고 발행 시각이 기록된다")
    void markPublished_SetsPublishedStatusAndTimestamp() {
        // given
        OutboxEvent event = OutboxEvent.create("ORDER_PAID", "{}");

        // when
        event.markPublished();

        // then
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(event.isPublished()).isTrue();
        assertThat(event.getPublishedAt()).isNotNull();
    }

    @Test
    @DisplayName("발행 실패가 최대 재시도 횟수 미만이면 PENDING 상태를 유지하며 다음 폴링에서 재시도된다")
    void markPublishFailed_BelowMaxRetry_StaysPending() {
        // given
        OutboxEvent event = OutboxEvent.create("ORDER_PAID", "{}");

        // when
        event.markPublishFailed();
        event.markPublishFailed();

        // then
        assertThat(event.getRetryCount()).isEqualTo(2);
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
    }

    @Test
    @DisplayName("발행 실패가 최대 재시도 횟수(5회)에 도달하면 FAILED로 확정되어 더 이상 재처리 대상이 아니다")
    void markPublishFailed_ReachesMaxRetry_BecomesFailed() {
        // given
        OutboxEvent event = OutboxEvent.create("ORDER_PAID", "{}");

        // when
        for (int i = 0; i < 5; i++) {
            event.markPublishFailed();
        }

        // then
        assertThat(event.getRetryCount()).isEqualTo(5);
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.FAILED);
    }
}
