package com.kkh.shop_1.domain.outbox.service;

/**
 *
 * 아웃박스 이벤트를 실제로 외부에 전달하는 방법을 추상화한 인터페이스.
 * 지금은 이메일 발송(EmailOutboxEventPublisher, ORDER_PAID만 처리)으로 구현되어 있고,
 * 다른 채널(푸시, 알림톡, 메시지 큐 등)이 필요해지면 이 인터페이스를 구현하는
 * @Component를 새로 만들어 교체하거나 이벤트 타입별로 분기하면 된다.
 *
 */
public interface OutboxEventPublisher {

    /**
     * @throws RuntimeException 발행 실패 시 던진다. (호출부에서 재시도/실패 카운트를 처리)
     */
    void publish(String eventType, String payload);
}
