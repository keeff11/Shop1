package com.kkh.shop_1.domain.item.repository;

import com.kkh.shop_1.domain.item.document.ItemDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface ItemSearchRepository extends ElasticsearchRepository<ItemDocument, Long> {

    List<ItemDocument> findTop10ByNameContainingOrNameChosungContaining(String keyword, String chosung);
}