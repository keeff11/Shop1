package com.kkh.shop_1.common.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableCaching // [중요] 스프링부트 캐싱 기능 활성화
public class RedisConfig {

    /**
     * RedisTemplate: Redis 데이터를 저장하고 조회하는 핵심 객체
     * Key는 String, Value는 JSON으로 직렬화하도록 설정
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Key는 문자열로 저장 (예: "items::all")
        template.setKeySerializer(new StringRedisSerializer());

        // Value는 JSON으로 저장 (사람이 읽을 수 있게)
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());

        return template;
    }
}