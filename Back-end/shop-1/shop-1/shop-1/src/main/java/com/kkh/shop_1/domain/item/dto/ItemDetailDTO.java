package com.kkh.shop_1.domain.item.dto;

import com.kkh.shop_1.domain.item.entity.Item;
import com.kkh.shop_1.domain.item.entity.ItemImage;
import lombok.*;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ItemDetailDTO {

    private Long id;
    private String name;
    private int price;
    private Integer discountPrice;
    private int quantity; // quantity 필드 추가
    private String category;
    private String description;
    private String stockStatus;
    private String status;
    private String sellerNickname;
    private String thumbnailUrl; // 썸네일도 상세에 필요할 수 있음
    private List<String> images;

    // ★ [추가] 프론트엔드 요구 필드
    private double averageRating;
    private int reviewCount;
    private int viewCount;
    private String createdAt; // 날짜 포맷팅 필요 시 String 변환 추천

    public static ItemDetailDTO from(Item item) {
        return ItemDetailDTO.builder()
                .id(item.getId())
                .name(item.getName())
                .price(item.getPrice())
                .discountPrice(item.getDiscountPrice())
                .quantity(item.getQuantity()) // quantity 매핑 추가
                .category(item.getItemCategory().name())
                .description(item.getDescription())
                .stockStatus(item.getStockStatus().name())
                .status(item.getStatus().name())
                .sellerNickname(item.getSeller().getNickname())
                .thumbnailUrl(item.getThumbnailUrl())
                .images(item.getImages().stream()
                        .map(ItemImage::getImageUrl)
                        .toList())
                // ★ [추가] 필드 매핑
                .averageRating(item.getAverageRating())
                .reviewCount(item.getReviewCount())
                .viewCount(item.getViewCount())
                .createdAt(item.getCreatedAt().toString()) // ISO 8601 형식
                .build();
    }
}