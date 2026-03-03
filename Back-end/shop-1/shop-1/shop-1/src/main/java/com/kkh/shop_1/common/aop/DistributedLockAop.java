package com.kkh.shop_1.common.aop;

import com.kkh.shop_1.common.annotation.DistributedLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import java.lang.reflect.Method;

@Order(1)
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class DistributedLockAop {
    private final RedissonClient redissonClient;
    private final AopForTransaction aopForTransaction;

    @Around("@annotation(com.kkh.shop_1.common.annotation.DistributedLock)")
    public Object lock(final ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        DistributedLock distributedLock = method.getAnnotation(DistributedLock.class);

        String key = "ITEM_LOCK:" + getDynamicKey(signature.getParameterNames(), joinPoint.getArgs(), distributedLock.key());
        RLock rLock = redissonClient.getLock(key);

        try {
            log.info("락 획득 시도: {}", key);
            boolean available = rLock.tryLock(distributedLock.waitTime(), distributedLock.leaseTime(), distributedLock.timeUnit());
            if (!available) {
                log.warn("락 획득 실패: {}", key);
                throw new IllegalStateException("현재 요청이 많아 처리가 지연되고 있습니다.");
            }
            return aopForTransaction.proceed(joinPoint);
        } finally {
            try {
                rLock.unlock();
                log.info("락 반납 완료: {}", key);
            } catch (IllegalMonitorStateException e) {
                log.info("이미 해제된 락입니다.");
            }
        }
    }

    private String getDynamicKey(String[] parameterNames, Object[] args, String keyName) {
        for (int i = 0; i < parameterNames.length; i++) {
            if (parameterNames[i].equals(keyName)) return String.valueOf(args[i]);
        }
        return keyName;
    }
}