package com.kkh.shop_1.domain.outbox.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 *
 * 트랜잭션 밖에서 처리해야 하는 부수 작업(알림 발송 등)을,
 * 상태 변경과 같은 트랜잭션으로 안전하게 기록해두기 위한 아웃박스 이벤트.
 *
 */
@Entity
@Table(name = "outbox_event")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent {

    private static final int MAX_RETRY_COUNT = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "outbox_event_id")
    private Long id;

    @Column(nullable = false, length = 100)
    private String eventType;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxStatus status;

    @Column(nullable = false)
    private int retryCount;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime publishedAt;

    @Builder(access = AccessLevel.PRIVATE)
    private OutboxEvent(String eventType, String payload) {
        this.eventType = eventType;
        this.payload = payload;
        this.status = OutboxStatus.PENDING;
        this.retryCount = 0;
    }

    public static OutboxEvent create(String eventType, String payload) {
        return OutboxEvent.builder()
                .eventType(eventType)
                .payload(payload)
                .build();
    }

    public boolean isPublished() {
        return this.status == OutboxStatus.PUBLISHED;
    }

    public void markPublished() {
        this.status = OutboxStatus.PUBLISHED;
        this.publishedAt = LocalDateTime.now();
    }

    /**
     * 발행 실패 처리. 최대 재시도 횟수를 넘기면 더 이상 재처리 대상이 되지 않도록 FAILED로 확정한다.
     */
    public void markPublishFailed() {
        this.retryCount++;
        if (this.retryCount >= MAX_RETRY_COUNT) {
            this.status = OutboxStatus.FAILED;
        }
    }
}
