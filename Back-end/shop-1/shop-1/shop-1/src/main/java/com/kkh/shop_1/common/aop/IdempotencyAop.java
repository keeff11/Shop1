package com.kkh.shop_1.common.aop;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kkh.shop_1.common.annotation.Idempotent;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 *
 * @Idempotent 메서드를 감싸서 Idempotency-Key 헤더 기준으로 중복 실행을 막는다.
 *
 * - 이미 완료된 요청(Redis에 결과가 캐시됨)이면 실제 로직을 실행하지 않고 캐시된 응답을 그대로 반환한다.
 * - 같은 키로 동시에 요청이 들어오는 경우, Redisson 분산 락(이 프로젝트에서 OrderService가 쓰는 것과 동일한 방식)으로
 *   한 요청만 실행하고 나머지는 즉시 거절한다. (대기/폴링 없이 fail-fast — OrderService의 결제 승인 락과 동일한 전략)
 * - 실패(예외 발생)한 요청은 캐시하지 않아, 같은 키로 재시도할 수 있게 한다.
 *
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class IdempotencyAop {

    private static final String HEADER_NAME = "Idempotency-Key";
    private static final String LOCK_KEY_PREFIX = "idempotency:lock:";
    private static final String RESULT_KEY_PREFIX = "idempotency:result:";
    private static final long LOCK_LEASE_SECONDS = 60L; // 실제 처리가 이보다 오래 걸리면 락이 풀릴 수 있음(재시도 시 재실행 가능성)

    private final RedissonClient redissonClient;
    private final StringRedisTemplate stringRedisTemplate;

    // Lombok @Getter만 있고 Setter/기본 생성자가 없는 DTO도 역직렬화할 수 있도록 필드 기준으로 접근한다.
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        String idempotencyKey = resolveIdempotencyKey();
        if (!StringUtils.hasText(idempotencyKey)) {
            throw new IllegalArgumentException("Idempotency-Key 헤더가 필요합니다.");
        }

        JavaType returnType = resolveReturnType(joinPoint);
        String resultKey = RESULT_KEY_PREFIX + idempotencyKey;

        Object cached = readCachedResult(resultKey, returnType);
        if (cached != null) {
            log.info("Idempotency 캐시 응답 반환. key={}", idempotencyKey);
            return cached;
        }

        RLock lock = redissonClient.getLock(LOCK_KEY_PREFIX + idempotencyKey);
        boolean isLocked;
        try {
            isLocked = lock.tryLock(0, LOCK_LEASE_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("요청 처리 중 인터럽트가 발생했습니다.");
        }

        if (!isLocked) {
            throw new IllegalStateException("동일한 요청이 이미 처리 중입니다. 잠시 후 다시 시도해주세요.");
        }

        try {
            // 락을 기다리는 동안 먼저 들어온 요청이 이미 처리를 끝냈을 수 있으므로 한 번 더 확인한다.
            cached = readCachedResult(resultKey, returnType);
            if (cached != null) {
                return cached;
            }

            Object result = joinPoint.proceed();
            cacheResult(resultKey, result, idempotent.ttlHours());
            return result;
        } finally {
            lock.unlock();
        }
    }

    private Object readCachedResult(String resultKey, JavaType returnType) {
        String cachedJson = stringRedisTemplate.opsForValue().get(resultKey);
        if (cachedJson == null) {
            return null;
        }
        try {
            return objectMapper.readValue(cachedJson, returnType);
        } catch (Exception e) {
            log.error("Idempotency 캐시 응답 역직렬화 실패. key={}", resultKey, e);
            return null;
        }
    }

    private void cacheResult(String resultKey, Object result, long ttlHours) {
        try {
            String json = objectMapper.writeValueAsString(result);
            stringRedisTemplate.opsForValue().set(resultKey, json, Duration.ofHours(ttlHours));
        } catch (Exception e) {
            // 응답 캐싱 실패는 멱등성 보장이 다음 재시도에서 안 될 뿐, 이번 요청 자체는 이미 성공했으므로 예외를 던지지 않는다.
            log.error("Idempotency 응답 캐싱 실패. key={}", resultKey, e);
        }
    }

    private JavaType resolveReturnType(ProceedingJoinPoint joinPoint) {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        return objectMapper.getTypeFactory().constructType(method.getGenericReturnType());
    }

    private String resolveIdempotencyKey() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        return request.getHeader(HEADER_NAME);
    }
}
