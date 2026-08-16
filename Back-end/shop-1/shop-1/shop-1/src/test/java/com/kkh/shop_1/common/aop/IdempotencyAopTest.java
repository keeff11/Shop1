package com.kkh.shop_1.common.aop;

import com.kkh.shop_1.common.annotation.Idempotent;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class IdempotencyAopTest {

    @InjectMocks
    private IdempotencyAop idempotencyAop;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    @Mock
    private RLock rLock;

    private Idempotent idempotent;

    // 실제 결제 응답 DTO(OrderDetailDTO 등)처럼 @Getter만 있고 기본 생성자가 protected인 형태를 재현한 응답 클래스
    static class SampleResponse {
        private Long id;
        private String message;

        protected SampleResponse() {
        }

        public SampleResponse(Long id, String message) {
            this.id = id;
            this.message = message;
        }

        public Long getId() {
            return id;
        }

        public String getMessage() {
            return message;
        }
    }

    interface TargetService {
        @Idempotent(ttlHours = 24)
        SampleResponse doWork();
    }

    @BeforeEach
    void setUp() throws NoSuchMethodException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Idempotency-Key", "test-key-1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        Method method = TargetService.class.getMethod("doWork");
        idempotent = method.getAnnotation(Idempotent.class);

        given(joinPoint.getSignature()).willReturn(methodSignature);
        given(methodSignature.getMethod()).willReturn(method);
        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("최초 요청은 실제 로직을 실행하고, 결과를 Redis에 캐싱한다")
    void firstRequest_ExecutesAndCachesResult() throws Throwable {
        // given
        given(valueOperations.get("idempotency:result:test-key-1")).willReturn(null);
        given(redissonClient.getLock("idempotency:lock:test-key-1")).willReturn(rLock);
        given(rLock.tryLock(0, 60, java.util.concurrent.TimeUnit.SECONDS)).willReturn(true);
        given(joinPoint.proceed()).willReturn(new SampleResponse(1L, "성공"));

        // when
        Object result = idempotencyAop.around(joinPoint, idempotent);

        // then
        assertThat(result).isInstanceOf(SampleResponse.class);
        assertThat(((SampleResponse) result).getMessage()).isEqualTo("성공");

        verify(joinPoint).proceed();
        verify(valueOperations).set(eq("idempotency:result:test-key-1"), any(String.class), eq(Duration.ofHours(24)));
        verify(rLock).unlock();
    }

    @Test
    @DisplayName("이미 처리된 키로 다시 요청하면 실제 로직을 실행하지 않고 캐시된 응답을 반환한다")
    void duplicateRequest_ReturnsCachedResultWithoutExecuting() throws Throwable {
        // given
        String cachedJson = "{\"id\":1,\"message\":\"이전 응답\"}";
        given(valueOperations.get("idempotency:result:test-key-1")).willReturn(cachedJson);

        // when
        Object result = idempotencyAop.around(joinPoint, idempotent);

        // then
        assertThat(result).isInstanceOf(SampleResponse.class);
        assertThat(((SampleResponse) result).getMessage()).isEqualTo("이전 응답");

        verify(joinPoint, never()).proceed();
        verify(redissonClient, never()).getLock(any(String.class));
    }

    @Test
    @DisplayName("같은 키로 동시에 요청이 들어오면(락 획득 실패) 즉시 예외를 던지고 실제 로직은 실행하지 않는다")
    void concurrentRequest_LockContention_ThrowsImmediately() throws Throwable {
        // given
        given(valueOperations.get("idempotency:result:test-key-1")).willReturn(null);
        given(redissonClient.getLock("idempotency:lock:test-key-1")).willReturn(rLock);
        given(rLock.tryLock(0, 60, java.util.concurrent.TimeUnit.SECONDS)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> idempotencyAop.around(joinPoint, idempotent))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 처리 중");

        verify(joinPoint, never()).proceed();
    }

    @Test
    @DisplayName("Idempotency-Key 헤더가 없으면 Redis를 확인하기 전에 즉시 예외를 던진다")
    void missingHeader_ThrowsBeforeTouchingRedis() {
        // given
        RequestContextHolder.resetRequestAttributes();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));

        // when & then
        assertThatThrownBy(() -> idempotencyAop.around(joinPoint, idempotent))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Idempotency-Key");

        verify(stringRedisTemplate, never()).opsForValue();
    }

    @Test
    @DisplayName("실제 로직이 실패하면 응답을 캐싱하지 않고, 같은 키로 재시도할 수 있도록 락을 해제한다")
    void executionFails_DoesNotCacheAndReleasesLock() throws Throwable {
        // given
        given(valueOperations.get("idempotency:result:test-key-1")).willReturn(null);
        given(redissonClient.getLock("idempotency:lock:test-key-1")).willReturn(rLock);
        given(rLock.tryLock(0, 60, java.util.concurrent.TimeUnit.SECONDS)).willReturn(true);
        given(joinPoint.proceed()).willThrow(new IllegalStateException("결제 승인 실패"));

        // when & then
        assertThatThrownBy(() -> idempotencyAop.around(joinPoint, idempotent))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("결제 승인 실패");

        verify(valueOperations, never()).set(any(String.class), any(String.class), any(Duration.class));
        verify(rLock).unlock();
    }
}
