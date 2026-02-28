package com.kkh.shop_1.domain.item.service;

import com.kkh.shop_1.common.util.ChosungUtils;
import com.kkh.shop_1.domain.item.document.ItemDocument;
import com.kkh.shop_1.domain.item.dto.ItemSummaryDTO;
import com.kkh.shop_1.domain.item.entity.Item;
import com.kkh.shop_1.domain.item.repository.ItemRepository;
import com.kkh.shop_1.domain.item.repository.ItemSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ItemSearchService {

    private final ItemSearchRepository itemSearchRepository;
    private final ItemRepository itemRepository;

    /**
     * [1] 기존 MySQL 데이터를 Elasticsearch로 전체 동기화 (초기 세팅용)
     */
    @Transactional(readOnly = true)
    public void syncItemsToElasticsearch() {
        itemSearchRepository.deleteAll(); // 기존 인덱스 초기화
        List<Item> items = itemRepository.findAll();

        List<ItemDocument> documents = items.stream()
                .map(item -> ItemDocument.from(item, ChosungUtils.extract(item.getName())))
                .toList();

        itemSearchRepository.saveAll(documents);
        log.info("총 {}개의 상품을 Elasticsearch에 동기화 완료했습니다.", documents.size());
    }

    /**
     * [2] 실시간 초성 및 단어 자동완성 검색
     */
    public List<ItemSummaryDTO> getAutocompleteSuggestions(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return List.of();
        }

        // 검색어의 초성 추출 (예: '나이ㅋ' -> 'ㄴㅇㅋ')
        String chosung = ChosungUtils.extract(keyword);

        // ES에서 일반 단어 또는 초성이 포함된 상품 Top 10 검색
        List<ItemDocument> documents = itemSearchRepository
                .findTop10ByNameContainingOrNameChosungContaining(keyword, chosung);

        return documents.stream()
                .map(doc -> ItemSummaryDTO.builder()
                        .id(doc.getId())
                        .name(doc.getName())
                        .price(doc.getPrice())
                        .thumbnailUrl(doc.getThumbnailUrl())
                        .build())
                .toList();
    }
}