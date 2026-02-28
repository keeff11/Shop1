package com.kkh.shop_1.domain.item.repository;

import com.kkh.shop_1.domain.item.document.ItemDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface ItemSearchRepository extends ElasticsearchRepository<ItemDocument, Long> {

    // 자동완성을 위해 검색어(일반 단어 OR 초성)가 포함된 상품을 최대 10개만 빠르게 가져오는 쿼리 메서드
    List<ItemDocument> findTop10ByNameContainingOrNameChosungContaining(String keyword, String chosung);
}