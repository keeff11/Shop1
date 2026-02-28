package com.kkh.shop_1.domain.item.document;

import com.kkh.shop_1.domain.item.entity.Item;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "items") // ES의 'items' 인덱스(테이블)에 매핑
public class ItemDocument {

    @Id
    private Long id; // MySQL의 Item ID와 동일하게 맞춤

    // 1. 일반 검색용 (형태소 분석기 nori 적용)
    @Field(type = FieldType.Text, analyzer = "nori")
    private String name;

    // 2. 초성 검색용 필드 (예: '나이키' -> 'ㄴㅇㅋ')
    @Field(type = FieldType.Text)
    private String nameChosung;

    @Field(type = FieldType.Keyword) // 정확히 일치할 때만 검색되도록 Keyword 타입 설정
    private String category;

    private Integer price;
    private String thumbnailUrl;

    // MySQL Item 엔티티를 ES ItemDocument로 변환
    public static ItemDocument from(Item item, String chosung) {
        return ItemDocument.builder()
                .id(item.getId())
                .name(item.getName())
                .nameChosung(chosung)
                .category(item.getItemCategory() != null ? item.getItemCategory().name() : "OTHERS")
                .price(item.getPrice())
                .thumbnailUrl(item.getThumbnailUrl())
                .build();
    }
}