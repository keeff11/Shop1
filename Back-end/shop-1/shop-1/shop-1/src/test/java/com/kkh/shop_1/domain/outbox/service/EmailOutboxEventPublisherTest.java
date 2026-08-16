package com.kkh.shop_1.domain.outbox.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kkh.shop_1.domain.user.entity.User;
import com.kkh.shop_1.domain.user.repository.UserRepository;
import com.kkh.shop_1.domain.user.service.EmailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailOutboxEventPublisherTest {

    private EmailOutboxEventPublisher publisher;

    @Mock
    private EmailService emailService;

    @Mock
    private UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("ORDER_PAID 이벤트는 결제자 이메일로 결제완료 알림 메일을 보낸다")
    void publish_OrderPaid_SendsEmailToBuyer() {
        // given
        publisher = new EmailOutboxEventPublisher(emailService, userRepository, objectMapper);

        User user = mock(User.class);
        given(user.getEmail()).willReturn("buyer@test.com");
        given(user.getNickname()).willReturn("구매자");
        given(userRepository.findById(100L)).willReturn(Optional.of(user));

        String payload = "{\"orderId\":1,\"userId\":100,\"totalAmount\":15000,\"paidAt\":\"2026-08-15T10:00:00\"}";

        // when
        publisher.publish("ORDER_PAID", payload);

        // then
        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendMail(org.mockito.ArgumentMatchers.eq("buyer@test.com"), subjectCaptor.capture(), contentCaptor.capture());

        assertThat(subjectCaptor.getValue()).contains("주문 #1");
        assertThat(contentCaptor.getValue()).contains("구매자").contains("15,000원");
    }

    @Test
    @DisplayName("ORDER_PAID가 아닌 이벤트 타입은 무시하고 메일을 보내지 않는다")
    void publish_OtherEventType_DoesNothing() {
        // given
        publisher = new EmailOutboxEventPublisher(emailService, userRepository, objectMapper);

        // when
        publisher.publish("SOME_OTHER_EVENT", "{}");

        // then
        verify(emailService, never()).sendMail(any(), any(), any());
        verify(userRepository, never()).findById(any());
    }

    @Test
    @DisplayName("대상 사용자를 찾을 수 없으면 예외를 던진다 (호출부가 재시도 카운트를 올릴 수 있도록)")
    void publish_UserNotFound_ThrowsException() {
        // given
        publisher = new EmailOutboxEventPublisher(emailService, userRepository, objectMapper);

        given(userRepository.findById(999L)).willReturn(Optional.empty());
        String payload = "{\"orderId\":1,\"userId\":999,\"totalAmount\":1000,\"paidAt\":\"2026-08-15T10:00:00\"}";

        // when & then
        assertThatThrownBy(() -> publisher.publish("ORDER_PAID", payload))
                .isInstanceOf(RuntimeException.class);

        verify(emailService, never()).sendMail(any(), any(), any());
    }
}
