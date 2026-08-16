package com.kkh.shop_1.domain.outbox.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kkh.shop_1.domain.user.entity.User;
import com.kkh.shop_1.domain.user.repository.UserRepository;
import com.kkh.shop_1.domain.user.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 *
 * outbox 이벤트를 실제 이메일로 발송하는 구현체 (LoggingOutboxEventPublisher를 대체).
 * 지금은 "ORDER_PAID" 이벤트만 처리하고, 그 외 이벤트 타입은 알림 없이 무시한다.
 * 다른 이벤트 타입/다른 채널(푸시, 알림톡 등)이 필요해지면 이 클래스를 분기하거나
 * OutboxEventPublisher 구현체를 이벤트 타입별로 나눠도 된다.
 *
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailOutboxEventPublisher implements OutboxEventPublisher {

    private final EmailService emailService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void publish(String eventType, String payload) {
        if (!"ORDER_PAID".equals(eventType)) {
            log.info("알림 대상이 아닌 이벤트 타입이라 건너뜁니다. eventType={}", eventType);
            return;
        }

        try {
            JsonNode node = objectMapper.readTree(payload);
            Long userId = node.get("userId").asLong();
            Long orderId = node.get("orderId").asLong();
            int totalAmount = node.get("totalAmount").asInt();

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalStateException("알림 대상 사용자를 찾을 수 없습니다. userId=" + userId));

            String subject = String.format("[Shop1] 주문 #%d 결제가 완료되었습니다", orderId);
            String content = String.format(
                    "%s님, 주문이 정상적으로 결제되었습니다.<br><br>주문번호: %d<br>결제금액: %,d원",
                    user.getNickname(), orderId, totalAmount
            );

            emailService.sendMail(user.getEmail(), subject, content);
            log.info("결제 완료 알림 이메일 발송 완료. orderId={}, userId={}", orderId, userId);

        } catch (Exception e) {
            throw new RuntimeException("결제 완료 알림 이메일 발송 실패. payload=" + payload, e);
        }
    }
}
