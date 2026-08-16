package com.kkh.shop_1.domain.outbox.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kkh.shop_1.domain.outbox.entity.OutboxEvent;
import com.kkh.shop_1.domain.outbox.entity.OutboxStatus;
import com.kkh.shop_1.domain.outbox.event.OutboxEventCreatedEvent;
import com.kkh.shop_1.domain.outbox.repository.OutboxEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class OutboxEventService {

    // 폴링 안전망이 재처리 대상으로 보는 기준: PENDING 상태로 이만큼 지난 이벤트 (즉시 발행이 아직 안 끝났을 수도 있는 시간은 건너뜀)
    private static final Duration STALE_THRESHOLD = Duration.ofMinutes(3);

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxEventPublisher outboxEventPublisher;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ObjectMapper objectMapper;

    // 항상 새 물리 트랜잭션을 강제하는 용도(REQUIRES_NEW)로만 쓰는 짧은 트랜잭션 템플릿.
    // tryPublish()에서 "DB 조회 -> 외부 I/O(트랜잭션 밖) -> DB 반영"을 명확히 분리하기 위해
    // 선언적 @Transactional 대신 프로그래밍 방식 트랜잭션을 사용한다.
    private final TransactionTemplate requiresNewTx;

    public OutboxEventService(OutboxEventRepository outboxEventRepository,
                               OutboxEventPublisher outboxEventPublisher,
                               ApplicationEventPublisher applicationEventPublisher,
                               ObjectMapper objectMapper,
                               PlatformTransactionManager transactionManager) {
        this.outboxEventRepository = outboxEventRepository;
        this.outboxEventPublisher = outboxEventPublisher;
        this.applicationEventPublisher = applicationEventPublisher;
        this.objectMapper = objectMapper;
        this.requiresNewTx = new TransactionTemplate(transactionManager);
        this.requiresNewTx.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
    }

    /**
     *
     * 주문/결제 상태 변경 트랜잭션 안에서 호출된다. 같은 트랜잭션으로 outbox 레코드를 남기고,
     * 커밋 후 즉시 발행을 시도할 수 있도록 스프링 이벤트를 함께 발행한다.
     *
     */
    @Transactional
    public Long record(String eventType, Object payload) {
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Outbox 이벤트 직렬화 실패: " + eventType, e);
        }

        OutboxEvent saved = outboxEventRepository.save(OutboxEvent.create(eventType, json));
        applicationEventPublisher.publishEvent(new OutboxEventCreatedEvent(saved.getId()));
        return saved.getId();
    }

    /**
     *
     * 단건 발행 시도. AFTER_COMMIT 즉시 발행과 폴링 스케줄러 양쪽에서 공통으로 사용한다.
     *
     * DB 조회 -> 외부 I/O(알림 발송, 트랜잭션 밖) -> DB 반영, 세 단계를 명확히 분리한다.
     * 본론(주문/결제)에서 얻은 교훈과 같다: 응답이 느릴 수 있는 외부 호출을 트랜잭션 안에 두면
     * 그동안 DB 커넥션이 반납되지 않는다. DB에 손대는 두 구간만 REQUIRES_NEW로 짧게 감싼다.
     *
     * REQUIRES_NEW인 이유: AFTER_COMMIT 콜백 시점엔 원래 트랜잭션이 이미 물리적으로 커밋됐지만
     * TransactionSynchronizationManager의 동기화 컨텍스트는 afterCompletion 전까지 아직 살아있다.
     * 기본 REQUIRED로는 이 상태에 "참여"하는 것으로 오인되어, 여기서 바꾼 status가 실제로는
     * 커밋되지 않고 유실되는 문제가 있었다. REQUIRES_NEW로 완전히 독립된 물리 트랜잭션을 강제한다.
     * 선언적 @Transactional은 같은 클래스 내부 호출(self-invocation)에는 적용되지 않으므로,
     * 여기서는 TransactionTemplate으로 직접 경계를 나눈다.
     *
     */
    public void tryPublish(Long outboxEventId) {
        OutboxEvent snapshot = requiresNewTx.execute(status ->
                outboxEventRepository.findById(outboxEventId).orElse(null));

        if (snapshot == null || snapshot.isPublished()) {
            return;
        }

        boolean success;
        try {
            outboxEventPublisher.publish(snapshot.getEventType(), snapshot.getPayload());
            success = true;
        } catch (Exception e) {
            log.error("Outbox 이벤트 발행 실패. id={}, eventType={}, retryCount={}",
                    snapshot.getId(), snapshot.getEventType(), snapshot.getRetryCount() + 1, e);
            success = false;
        }

        applyPublishResult(outboxEventId, success);
    }

    private void applyPublishResult(Long outboxEventId, boolean success) {
        requiresNewTx.executeWithoutResult(status -> {
            OutboxEvent event = outboxEventRepository.findById(outboxEventId).orElse(null);
            if (event == null) {
                return;
            }
            if (success) {
                event.markPublished();
            } else {
                event.markPublishFailed();
            }
            outboxEventRepository.save(event);
        });
    }

    /**
     *
     * 폴링 안전망 대상 조회: PENDING 상태이면서 일정 시간 이상 지난 이벤트
     * (즉시 발행 실패, 또는 커밋 직후 애플리케이션이 재시작되어 이벤트 리스너가 아예 못 뜬 경우 등)
     *
     */
    @Transactional(readOnly = true)
    public List<Long> findStalePendingEventIds() {
        LocalDateTime threshold = LocalDateTime.now().minus(STALE_THRESHOLD);
        return outboxEventRepository.findByStatusAndCreatedAtBefore(OutboxStatus.PENDING, threshold).stream()
                .map(OutboxEvent::getId)
                .toList();
    }
}
