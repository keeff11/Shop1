package com.kkh.shop_1.domain.item.dto;

import com.kkh.shop_1.domain.item.entity.Item;
import lombok.*;

import java.io.Serializable; // [1. 추가]

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ItemSummaryDTO implements Serializable { // [2. 추가] implements Serializable

    private static final long serialVersionUID = 1L; // [3. 권장] 버전 관리를 위한 ID 추가

    private Long id;
    private String name;
    private int price;
    private String category;
    private Integer discountPrice;
    private String stockStatus;
    private String status;
    private String thumbnailUrl;

    /**
     * Item 엔티티를 요약 DTO로 변환
     */
    public static ItemSummaryDTO from(Item item) {
        return ItemSummaryDTO.builder()
                .id(item.getId())
                .name(item.getName())
                .price(item.getPrice())
                .category(item.getItemCategory().name())
                .discountPrice(item.getDiscountPrice())
                .stockStatus(item.getStockStatus().name())
                .status(item.getStatus().name())
                .thumbnailUrl(item.getThumbnailUrl() != null ? item.getThumbnailUrl() : "/no_image.jpg")
                .build();
    }
}