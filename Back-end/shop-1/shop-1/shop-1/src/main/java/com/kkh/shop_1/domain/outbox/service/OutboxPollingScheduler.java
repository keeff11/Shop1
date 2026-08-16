package com.kkh.shop_1.domain.outbox.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 *
 * 즉시 발행(AFTER_COMMIT)이 실패했거나, 발행 시도 자체가 누락된(애플리케이션 재시작 등) PENDING 이벤트를
 * 주기적으로 재처리하는 폴링 안전망.
 *
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPollingScheduler {

    private final OutboxEventService outboxEventService;

    @Scheduled(fixedDelay = 60_000L) // 1분마다
    public void republishStaleEvents() {
        List<Long> staleEventIds = outboxEventService.findStalePendingEventIds();
        if (staleEventIds.isEmpty()) {
            return;
        }

        log.info("Outbox 폴링 안전망: 재처리 대상 {}건", staleEventIds.size());
        staleEventIds.forEach(outboxEventService::tryPublish);
    }
}
