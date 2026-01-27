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
    private int quantity;
    private String category;
    private String description;
    private String stockStatus;
    private String status;
    private Long sellerId;

    private String sellerNickname;
    private String thumbnailUrl;
    private List<String> images;
    private double averageRating;
    private int reviewCount;
    private int viewCount;
    private String createdAt;

    public static ItemDetailDTO from(Item item) {
        return ItemDetailDTO.builder()
                .id(item.getId())
                .name(item.getName())
                .price(item.getPrice())
                .discountPrice(item.getDiscountPrice())
                .quantity(item.getQuantity())
                .category(item.getItemCategory().name())
                .description(item.getDescription())
                .stockStatus(item.getStockStatus().name())
                .status(item.getStatus().name())

                // [중요] 여기서 sellerId를 반드시 매핑해야 합니다.
                .sellerId(item.getSeller().getId())

                .sellerNickname(item.getSeller().getNickname())
                .thumbnailUrl(item.getThumbnailUrl())
                .images(item.getImages().stream()
                        .map(ItemImage::getImageUrl)
                        .toList())
                .averageRating(item.getAverageRating())
                .reviewCount(item.getReviewCount())
                .viewCount(item.getViewCount())
                .createdAt(item.getCreatedAt().toString())
                .build();
    }
}