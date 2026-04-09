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
@Document(indexName = "items")
public class ItemDocument {

    @Id
    private Long id;

    @Field(type = FieldType.Text, analyzer = "nori")
    private String name;

    @Field(type = FieldType.Text)
    private String nameChosung;

    @Field(type = FieldType.Keyword)
    private String category;

    private Integer price;
    private String thumbnailUrl;

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