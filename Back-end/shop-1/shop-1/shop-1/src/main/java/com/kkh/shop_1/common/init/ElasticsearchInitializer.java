package com.kkh.shop_1.common.init;

import com.kkh.shop_1.domain.item.service.ItemSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ElasticsearchInitializer {

    private final ItemSearchService itemSearchService;

    /**
     * 스프링 부트 서버가 완전히 켜진 직후(ApplicationReadyEvent에 자동 실행
     */
    @EventListener(ApplicationReadyEvent.class)
    public void syncDataOnStartup() {
        try {
            log.info("ℹ️ [Elasticsearch] 서버 시작: MySQL -> Elasticsearch 데이터 동기화 시작...");

            // 기존 컨트롤러에서 호출하던 동기화 서비스 메서드를 그대로 실행
            itemSearchService.syncItemsToElasticsearch();

            log.info("✅ [Elasticsearch] 데이터 동기화 완료!");
        } catch (Exception e) {
            log.error("❌ [Elasticsearch] 데이터 동기화 중 오류 발생: {}", e.getMessage());
        }
    }
}