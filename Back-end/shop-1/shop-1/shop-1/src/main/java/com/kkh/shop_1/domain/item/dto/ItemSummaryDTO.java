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
    private String category; // [추가] 카테고리 필드
    private Integer discountPrice; // (선택) 목록에 할인 가격도 보여주려면 추가
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
                .category(item.getItemCategory().name()) // [추가] 엔티티에서 카테고리 이름 가져오기
                .discountPrice(item.getDiscountPrice()) // (선택)
                .stockStatus(item.getStockStatus().name())
                .status(item.getStatus().name())
                .thumbnailUrl(item.getThumbnailUrl() != null ? item.getThumbnailUrl() : "/no_image.jpg")
                .build();
    }
}