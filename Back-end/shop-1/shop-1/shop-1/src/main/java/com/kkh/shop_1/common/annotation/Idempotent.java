package com.kkh.shop_1.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * 클라이언트가 보낸 Idempotency-Key 헤더를 기준으로 멱등성을 보장한다.
 * 같은 키로 이미 성공 처리된 요청이면 메서드를 다시 실행하지 않고 저장된 결과를 그대로 반환한다.
 *
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

    /**
     * 처리 결과를 Redis에 보관할 시간 (시간 단위)
     */
    long ttlHours() default 24L;
}
