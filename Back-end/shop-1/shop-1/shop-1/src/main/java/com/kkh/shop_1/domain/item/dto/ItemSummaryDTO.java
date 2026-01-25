package com.kkh.shop_1.domain.item.dto;

import com.kkh.shop_1.domain.item.entity.Item;
import lombok.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ItemSummaryDTO {

    private Long id;
    private String name;
    private int price;
    private String stockStatus;
    private String status;
    private String thumbnailUrl;

    /**
     *
     * Item 엔티티를 요약 DTO로 변환
     *
     */
    public static ItemSummaryDTO from(Item item) {
        return ItemSummaryDTO.builder()
                .id(item.getId())
                .name(item.getName())
                .price(item.getPrice())
                .stockStatus(item.getStockStatus().name())
                .status(item.getStatus().name())
                .thumbnailUrl(item.getThumbnailUrl() != null ? item.getThumbnailUrl() : "/no_image.jpg")
                .build();
    }
}